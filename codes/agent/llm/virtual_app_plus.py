import http.client
import json
import copy
import re
import os
import random
from load_page import load_data, PAGE_FOLDER_PATH
import json
import requests
import subprocess
from openai import OpenAI
import httpx
import random
import evaluate
import sys, getopt
import time
import argparse

from http import HTTPStatus
import dashscope

HISTORY_OPT = []  # (page,instruct,action,parameter)
PAGE_COOKIES = []
PAGE_SUMMARY_PATH = "Alipay/summary/page_summary.json"
BAICHUAN_API_KEY = "sk-7a61da18cca7b2d454000af8e0b17f3e"
BAICHUAN_MODELS = ["Baichuan2-turbo", "Baichuan4"]


def judge_type_task(golden_steps):
    for step in golden_steps:
        if step[2] == 'type':
            return True
    return False


def interaction(page, user_prompt):
    # 如果页面停留不变，不刷新状态
    if len(PAGE_COOKIES) == 0 or PAGE_COOKIES[-1] != page.page_name:
        page_status = generate_new_page_status(page)
        PAGE_COOKIES.append(page_status)
    page_status = PAGE_COOKIES[-1]

    optional_instruct_set = get_optional_instruct_set(page, page_status)
    optional_instruct_set_text = instruct_set2text(optional_instruct_set)

    role_prompt = f"""你是一个智能移动应用助手，可以在所提供的页面指令中进行选择，以达成用户的目标。"""
    page_prompt = f"""以下是一个页面的例子：
当前页面名称: %s"
可用指令（指令名称，动作类型）: %s""" % (page_status[0], str(optional_instruct_set_text))
    instruct_rule_prompt = f"""请在可选指令中选择下一步操作，格式为(指令名称,动作类型,参数)
    参数指的是当指令动作类型为type时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段，例如(扫一扫, click, )"""
    example_prompt = "以下是一个完整的例子：" \
                     "提问：" \
                     "当前页面名称: <页面名称>" \
                     "可用指令（指令名称，动作类型）: <可用指令序列>" \
                     "回复：" \
                     "(扫一扫, click, )"
    sys_prompt = role_prompt + "\n" + page_prompt + "\n" + instruct_rule_prompt + "\n"

    print(sys_prompt)
    # msg = input(sys_prompt)
    gpt_messages = [
        {"role": "system", "content": sys_prompt},
        {"role": "user", "content": user_prompt}
    ]
    msg = chat_to_GPT(gpt_messages)
    error_flag, res, _ = verify_reply(msg, page, "")

    next_page_name = page.page_name
    if not error_flag:
        instruct_name, action, parameter = res
        update_page_status(page, instruct_name)
        HISTORY_OPT.append((page.page_name, instruct_name, action, parameter))
        next_page_name = page.instruct_dict[instruct_name].next_page_name

    print(HISTORY_OPT)
    return next_page_name, res


def generate_new_page_status(page):
    instruct_list = list(page.instruct_dict.keys())
    is_enable_list = [1] * len(instruct_list)  # 0 unable, 1 enable
    is_used_list = [0] * len(instruct_list)  # 0 unused, 1 used
    type_flag = False
    for instruct in instruct_list:
        action = page.instruct_dict[instruct].action
        if action == "type":
            type_flag = True
            break
    if type_flag:  # 如果页面中有需要输入的内容，将点击动作和人为动作设为unable
        for i in range(0, len(instruct_list)):
            instruct_name = instruct_list[i]
            action = page.instruct_dict[instruct_name].action
            if not action == "type":
                is_enable_list[i] = 0
    return page.page_name, instruct_list, is_enable_list, is_used_list


def get_optional_instruct_set(page, page_status):
    is_enable_list = page_status[2]
    is_used_list = page_status[3]

    # 关闭返回按钮
    instruct_list = page_status[1]
    for i in range(0, len(instruct_list)):
        if instruct_list[i] == '返回':
            is_enable_list[i] = 0
    for i in range(0, len(is_used_list)):
        if is_used_list[i] == 1:
            is_enable_list[i] = 0

    optional_instruct_set = {}
    for i in range(0, len(is_enable_list)):
        if is_enable_list[i] == 1:
            optional_instruct_set[instruct_list[i]] = page.instruct_dict[instruct_list[i]]
    return optional_instruct_set


def instruct_set2text(instrcut_set):
    texts = []
    for instruct_name in instrcut_set.keys():
        item = (instruct_name, instrcut_set[instruct_name].action)
        texts.append(item)
    return texts


def get_msg_content_gpt(msg):
    # 输入字符串
    input_string = str(msg)

    # 定义正则表达式模式
    pattern = r"content='(.*?)'"

    # 使用正则表达式匹配
    match = re.search(pattern, input_string)

    # 提取匹配的内容
    if match:
        content_value = match.group(1)
        return content_value.strip()
    else:
        return ""


def get_msg_content_llama(msg):
    # 输入字符串
    input_string = str(msg)

    # 定义正则表达式模式
    pattern = r"\"content\":'(.*?)'"

    # 使用正则表达式匹配
    match = re.search(pattern, input_string)

    # 提取匹配的内容
    if match:
        content_value = match.group(1)
        return content_value.strip()
    else:
        return ""


def get_msg_content_qwen(msg):
    res = msg["output"]["choices"][0]["message"]['content']
    # print(res)
    return res


def get_msg_content_baichuan(msg):
    return msg


def change_history_gpt2llama(gpt_messages):
    llama_message = []
    for msg in gpt_messages:
        if msg["role"] == "system":
            msg["role"] = "Human"
        if msg["role"] == "assistant":
            msg["role"] = "Assistant"
        if msg["role"] == "user":
            msg["role"] = "Human"
    tmp_msg = gpt_messages[0]
    for i in range(1, len(gpt_messages)):
        if gpt_messages[i]["role"] == tmp_msg["role"]:
            tmp_msg["content"] = tmp_msg["content"] + gpt_messages[i]["content"]
        else:
            llama_message.append(tmp_msg)
            tmp_msg = gpt_messages[i]
    llama_message.append(tmp_msg)
    return llama_message


def verify_reply(msg, page, optional_instruct_set_text):
    error_flag = 0
    error_msg = ""
    # input_string = get_msg_content_gpt(str(msg)).strip()
    input_string = msg[msg.find("("):msg.find(")") + 1].strip()
    instruct_name = ""
    action = ""
    parameter = ""
    res = tuple(input_string[1:-1].split(","))
    # # 定义正则表达式模式
    # pattern = r"\<指令名称:(?P<command_name>.*?)\>\<动作类型:(?P<action_type>.*?)\>\<参数: (?P<parameters>.*)\>', role='"
    #
    # # 使用正则表达式匹配
    # match = re.search(pattern, input_string)
    #
    # # 提取匹配的信息
    # if not match:
    #     re_ERROR = f"""格式为(指令名称,动作类型,参数)
    # 参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段，例如(扫一扫, click, )
    #     请重试。"""
    #     print(re_ERROR)
    #     error_msg = re_ERROR
    #     error_flag = 1
    # else:
    #     instruct_name = match.group('command_name')
    #     action = match.group('action_type')
    #     parameter = match.group('parameters')
    try:
        instruct_name = res[0].strip()
        action = res[1].strip()
        if len(res) == 2:
            parameter = ""
        else:
            parameter = res[2].strip()
        if action == "input":
            action = "type"
        if action == "type":
            if parameter[0] == "\"" and parameter[-1] == "\"":
                parameter = parameter[1:-1]
    except IndexError:
        # re_ERROR = f"""格式为(指令名称,动作类型,参数)。参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段，例如(扫一扫, click, )，请重试。"""
        re_ERROR = f"""只需回答(指令名称,动作类型,参数)，包含半角括号和逗号，仅需根据页面可选指令进行操作。请重试。"""
        # print(re_ERROR)
        error_msg = re_ERROR
        error_flag = 1
    else:
        # print(f"{instruct_name},{action},{parameter}")
        if not instruct_name in page.instruct_dict:
            instruct_ERROR = f"页面中不存在该指令，请重试。页面中的可选指令列表为{optional_instruct_set_text}"
            error_msg = instruct_ERROR
            # print(instruct_ERROR)
            error_flag = 1
        elif not action == page.instruct_dict[instruct_name].action:
            action_ERROR = "该指令动作类型与指令不匹配，请重试。"
            error_msg = action_ERROR
            # print(action_ERROR)
            error_flag = 1
        elif action == "type" and parameter == "":
            type_ERROR = "未输入内容，请重试。"
            error_msg = type_ERROR
            # print(type_ERROR)
            error_flag = 1
    finally:
        if error_flag:
            return error_flag, (), error_msg
        else:
            return error_flag, (instruct_name, action, parameter), error_msg


def chat_to_GPT(gpt_message, GPTmodel="gpt-3.5-turbo"):
    # client = OpenAI(
    #     base_url="https://oneapi.xty.app/v1",
    #     api_key="sk-1YNU7ZLhGNWh3ejg6cBd12F9D36c48Fc83F88a5a06A6801f",
    #     http_client=httpx.Client(
    #         base_url="https://oneapi.xty.app/v1",
    #         follow_redirects=True,
    #     ),
    # )
    print(gpt_message)
    client = OpenAI(
        base_url="https://oneapi.xty.app/v1",
        api_key="sk-ao29u5l1QaGZqxQQE9Ae02331d2d4b42A05e82338067Da3e",
        http_client=httpx.Client(
            base_url="https://oneapi.xty.app/v1",
            follow_redirects=True,
        ),
    )

    completion = client.chat.completions.create(
        model=GPTmodel,
        messages=gpt_message,
        temperature=0.3
    )
    # print(completion.choices[0].message)
    # pattern = r"content='(.*)', role"
    # reg1 = re.compile(pattern)
    # res = reg1.findall(str(completion.choices[0].message))
    # print(res[0])
    return completion.choices[0].message


def chat_to_llama(message):
    conn = http.client.HTTPSConnection("llama.atomecho.cn")
    payload = json.dumps({
        "param": {
            "stream": False
        },
        "messages": message
    })
    # print(message)
    headers = {
        'S-Auth-Secret': '',
        'S-Auth-ApiKey': '',
        'Content-Type': 'application/json'
    }
    conn.request("POST", "/api/v1/chat/completion?endpoint=a1a4c140-b213-4e-gic", payload, headers)
    res = conn.getresponse()
    data = res.read()
    print(data.decode("utf-8"))
    return data.decode("utf-8")


def chat_to_qwen(messages, LLMmodel):
    dashscope.api_key = "sk-c900942e4ef544a6b33e77a0ef2ec370"
    # print(messages)
    times = 0
    while times < 3:
        try:
            response = dashscope.Generation.call(
                model=LLMmodel,
                messages=messages,
                result_format='message',  # set the result to be "message" format.
            )
            if response.status_code == HTTPStatus.OK:
                return response
            else:
                print('Request id: %s, Status code: %s, error code: %s, error message: %s' % (
                    response.request_id, response.status_code,
                    response.code, response.message
                ))
        except Exception:
            print("qwen exception")
            times = times + 1


def chat_to_baichuan(messages, LLMmodel):
    # API 请求 URL
    url = "https://api.baichuan-ai.com/v1/chat/completions"

    # 请求头部参数
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {BAICHUAN_API_KEY}"
    }

    # 构建对话消息

    # 请求体参数
    data = {
        "model": LLMmodel,
        "messages": messages,
        "stream": False,
        "temperature": 0.3,
        "top_p": 0.85,
        "top_k": 5,
        "with_search_enhance": False
        # 添加其他参数...
    }

    # 发送 POST 请求
    response = requests.post(url, headers=headers, json=data, timeout=60)

    # 处理响应
    if response.status_code == 200:
        # 使用 jq 美化 JSON 输出
        result = json.loads(response.text)
        formatted_result = subprocess.run(['jq', '.'], input=json.dumps(result), text=True, capture_output=True)

        # 输出助手的回复
        # print(f"助手: {formatted_result.stdout}")
        formatted_result_json = json.loads(formatted_result.stdout)
        return formatted_result_json['choices'][0]['message']['content']

    else:
        print(f"请求失败，状态码: {response.status_code}")


def chat_to_baichuan_openaiSDK(messages, LLMmodel):
    client = OpenAI(
        api_key=BAICHUAN_API_KEY,
        base_url="https://api.baichuan-ai.com/v1/",
    )

    completion = client.chat.completions.create(
        model=LLMmodel,
        messages=messages,
    )

    # for chunk in completion:
    #     print(chunk.choices[0].delta)

    return completion.choices[0].message


def update_page_status(page, instruct_name):
    page_name = PAGE_COOKIES[-1][0]
    instruct_list = PAGE_COOKIES[-1][1]
    is_enable_list = PAGE_COOKIES[-1][2]
    is_used_list = PAGE_COOKIES[-1][3]

    index = instruct_list.index(instruct_name)
    is_used_list[index] = 1
    is_enable_list_new = check_enable(page, instruct_list, is_used_list, is_enable_list)
    PAGE_COOKIES.pop()
    PAGE_COOKIES.append((page_name, instruct_list, is_enable_list_new, is_used_list))
    return


def check_enable(page, instruct_list, is_used_list, is_enable_list):
    type_index_list = []
    change_flag = True
    for i in range(0, len(instruct_list)):
        action = page.instruct_dict[instruct_list[i]].action
        if action == "type" and is_used_list[i] == 0:
            change_flag = False

    if change_flag:  # 如果页面中有需要输入的内容，将点击动作和人为动作设为unable
        is_enable_list = [1] * len(instruct_list)
    return is_enable_list


def continuous_interaction(query, function, page_dict, page_summary, LLMmodel, is_query_summary=False,
                           is_page_summary=False, is_predictive=False, is_error_reply=False):
    page_name = "首页"

    role_prompt = f"""你是一个智能移动应用助手，可以在所提供的页面指令中进行选择，以达成用户的目标。"""
    page_rule_prompt = f"""页面将以以下格式提供。
    当前页面名称: " 
    可选指令（指令名称，动作类型）: """
    instruct_rule_prompt = f"""仅需要根据可选指令中选择下一步操作，格式为(指令名称,动作类型,参数)
    参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段。例如(扫一扫, click, )"""
    sys_prompt = role_prompt + "\n" + page_rule_prompt + "\n" + instruct_rule_prompt + "\n"

    last_decision = ""
    last_decision_reply = ""
    last_decision_flag = False
    counter = 1
    while counter < 15:  # 最大试错步数
        history_prompt = f"历史操作指令:{HISTORY_OPT}"
        last_decision_prompt = f"上次的决策:{last_decision}。对此决策的回复:{last_decision_reply}"
        conversation_history = []
        conversation_history.append({"role": "system", "content": sys_prompt})
        try:
            page = page_dict[page_name]
        except KeyError:
            print(f"【错误】不存在页面：{page_name}")
            break
        # 生成新的user prompt
        if len(PAGE_COOKIES) == 0 or PAGE_COOKIES[-1][0] != page.page_name:
            page_status = generate_new_page_status(page)
            PAGE_COOKIES.append(page_status)
        page_status = PAGE_COOKIES[-1]

        optional_instruct_set = get_optional_instruct_set(page, page_status)
        optional_instruct_set_text = instruct_set2text(optional_instruct_set)

        # 若有执行了输入指令，将其更新到页面上
        for option in HISTORY_OPT:
            if option[0] == page_name and option[2] == 'type':
                for i in range(0, len(optional_instruct_set_text)):
                    if optional_instruct_set_text[i][0] == option[1] and optional_instruct_set_text[i][1] == option[2]:
                        optional_instruct_set_text[i] = (option[1], option[2], option[3])

        observation_prompt = f"""
                当前页面名称: %s" 
                可用指令（指令名称，动作类型）: %s""" % (page_status[0], str(optional_instruct_set_text))
        if is_page_summary:
            if page_summary.__contains__(page_name):
                page_summary_prompt = "\n页面摘要：" + page_summary[page_name]
                observation_prompt = observation_prompt + page_summary_prompt
        if is_query_summary == False:
            user_prompt = query + "\n" + observation_prompt + "\n" + history_prompt
        else:
            user_prompt = query + "\n意图：" + function + "\n" + observation_prompt + "\n" + history_prompt
        if last_decision_flag:
            user_prompt = user_prompt + "\n" + last_decision_prompt
        conversation_history.append({"role": "user", "content": user_prompt})

        # 传入gpt得到初次决策
        print(conversation_history)

        if LLMmodel in ['gpt-3.5-turbo', "gpt-3.5-turbo-1106", "gpt-4"]:
            msg = chat_to_GPT(conversation_history, LLMmodel)
            LLM_reply = get_msg_content_gpt(msg)
        elif LLMmodel == "llama":
            llama_conversation_history = change_history_gpt2llama(conversation_history)
            msg = chat_to_llama(llama_conversation_history)
            LLM_reply = get_msg_content_llama(msg)
        elif LLMmodel in ["qwen-turbo", "qwen1.5-72b-chat"]:
            msg = chat_to_qwen(conversation_history, LLMmodel)
            LLM_reply = get_msg_content_qwen(msg)
        elif LLMmodel in BAICHUAN_MODELS:
            msg = chat_to_baichuan_openaiSDK(conversation_history, LLMmodel)
            LLM_reply = get_msg_content_gpt(msg)

        conversation_history.append({"role": "assistant", "content": LLM_reply})
        print(LLM_reply)

        # 验证是否有错误
        error_flag, res, error_msg = verify_reply(LLM_reply, page, optional_instruct_set_text)
        last_decision = LLM_reply
        # if error_flag:
        #     if is_error_reply:
        #         # conversation_history.append({"role": "user", "content": error_msg})
        #         last_decision_reply = error_msg
        #     else:
        #         # conversation_history.append({"role": "user", "content": "请重试"})
        #         last_decision_reply = "出错了，请重试。"

        # 预见自纠
        retry_flag = False
        if error_flag == False and is_predictive:
            instruct_name, action, parameter = res
            predict_next_page_name = page.instruct_dict[instruct_name].next_page_name
            if predict_next_page_name != page_name:
                if page_summary.__contains__(predict_next_page_name):
                    predictive_obs_prompt = "执行{}指令会使得页面跳转至{}，新页面摘要：{}。".format(str(res), predict_next_page_name,
                                                                                page_summary[predict_next_page_name])
                else:
                    predictive_obs_prompt = "执行{}指令会使得页面跳转至{}。".format(str(res), predict_next_page_name)
            else:
                if action == "human":
                    predictive_obs_prompt = "执行{}指令后会结束该任务。".format(str(res))
                else:
                    predictive_obs_prompt = "执行{}指令会停留在页面{}上。".format(str(res), predict_next_page_name)
            predictive_prompt = predictive_obs_prompt + "是否确认执行？回复确认或取消。"
            print(predictive_prompt)
            conversation_history.append({"role": "user", "content": predictive_prompt})

            if LLMmodel in ['gpt-3.5-turbo', "gpt-3.5-turbo-1106", "gpt-4"]:
                msg = chat_to_GPT(conversation_history, LLMmodel)
                LLM_reply = get_msg_content_gpt(msg)
            elif LLMmodel == "llama":
                llama_conversation_history = change_history_gpt2llama(conversation_history)
                msg = chat_to_llama(llama_conversation_history)
                LLM_reply = get_msg_content_llama(msg)
            elif LLMmodel in ["qwen-turbo", "qwen1.5-72b-chat"]:
                msg = chat_to_qwen(conversation_history, LLMmodel)
                LLM_reply = get_msg_content_gpt(msg)
            elif LLMmodel in BAICHUAN_MODELS:
                msg = chat_to_baichuan_openaiSDK(conversation_history, LLMmodel)
                LLM_reply = get_msg_content_gpt(msg)
            print(LLM_reply)
            # conversation_history.append({"role": "assistant", "content": LLM_reply})
            if LLM_reply == "取消":
                retry_flag = True

        # 页面状态更新及跳转
        if not retry_flag:
            next_page_name = page_name
            if error_flag:
                if is_error_reply:
                    # conversation_history.append({"role": "user", "content": error_msg})
                    last_decision_reply = error_msg
                else:
                    # conversation_history.append({"role": "user", "content": "请重试"})
                    last_decision_reply = "出错了，请重试。"
            else:
                instruct_name, action, parameter = res
                update_page_status(page, instruct_name)
                HISTORY_OPT.append((page.page_name, instruct_name, action, parameter))
                next_page_name = page.instruct_dict[instruct_name].next_page_name
                last_decision_reply = "已成功执行"
            page_name = next_page_name

            # 检验结束条件
            counter += 1
            if res:
                if res[1] == "human":
                    break
        else:  # 如果二次确认为取消
            last_decision_reply = "决定不执行该指令，请尝试其他指令。"
        last_decision_flag = True

    print(HISTORY_OPT)
    return (HISTORY_OPT)


def no_countinous_interactiion(query):
    page_dict = load_data(PAGE_FOLDER_PATH)
    page = page_dict["首页"]
    user_prompt = query
    page_name, instruct = interaction(page, user_prompt)
    counter = 1
    while counter < 8:
        if instruct:
            page_name, instruct = interaction(page_dict[page_name], user_prompt)
            print(instruct)
            counter += 1
        if instruct[1] == "human":
            break
    print(HISTORY_OPT)


def load_task_json(json_file_path, is_query_summary=False):
    task_list = []
    with open(json_file_path, 'r') as file:
        tasks = json.load(file)
    if not is_query_summary:
        for task in tasks['tasks']:
            query = task['query']
            answer = []
            for step in task['answer']:
                answer.append((step["pageName"], step["instruct"], step["action"], step["parameter"]))
            task_list.append((query, answer))
        return task_list
    else:
        for task in tasks['tasks']:
            query = task['query']
            function = task['function']
            answer = task['answer']
            # for step in task['answer']:
            #     answer.append((step["pageName"], step["instruct"], step["action"], step["parameter"]))
            task_list.append((query, answer, function))
        return task_list


def random_interaction(page_dict):
    page_name = "首页"
    counter = 1
    while counter < 10:
        try:
            page = page_dict[page_name]
        except KeyError:
            print(f"【错误】不存在页面：{page_name}")
            break
        if len(PAGE_COOKIES) == 0 or PAGE_COOKIES[-1][0] != page.page_name:
            page_status = generate_new_page_status(page)
            PAGE_COOKIES.append(page_status)
        page_status = PAGE_COOKIES[-1]

        optional_instruct_set = get_optional_instruct_set(page, page_status)
        optional_instruct_set_text = instruct_set2text(optional_instruct_set)

        res = random.choice(optional_instruct_set_text)

        if len(res) == 2:
            instruct_name, action = res
        else:
            instruct_name, action, _ = res
        parameter = ""
        update_page_status(page, instruct_name)
        HISTORY_OPT.append((page.page_name, instruct_name, action, parameter))
        next_page_name = page.instruct_dict[instruct_name].next_page_name
        page_name = next_page_name

        # 检验结束条件
        counter += 1
        if res:
            if res[1] == "human":
                break

    print(HISTORY_OPT)
    return (HISTORY_OPT)


def batch_data(json_file_path, query_summary_task_path, page_summary_path, page_folder_path, LLMmodel, is_query_summary,
               is_page_summary, is_predictive, is_error_reply, is_random):
    page_dict = load_data(page_folder_path)
    page_summary = load_page_summary(page_dict, page_summary_path, LLMmodel, summary_word_num=30)

    task_list = []
    # 用户需求总结设置
    if is_query_summary == False:
        task_list = load_task_json(json_file_path)
    else:
        task_list = query_summary_tasks(query_summary_task_path, json_file_path, LLMmodel)
    random.shuffle(task_list)

    # 统计数据集信息
    correct = 0
    task_num = len(task_list)
    type_task_num = 0
    correct_type_task_num = 0

    type_instruct_total = 0
    select_instruct_total = 0
    click_instruct_total = 0
    human_instruct_total = 0
    type_instruct_correct_total = 0
    select_instruct_correct_total = 0
    click_instruct_correct_total = 0
    human_instruct_correct_total = 0

    total_cost_time = 0
    total_correct_cost_time = 0
    total_wrong_cost_time = 0

    total_instruct_average_cost_time = 0
    total_instruct_average_correct_cost_time = 0
    total_instruct_average_wrong_cost_time = 0

    for task in task_list:
        start_time = time.time()
        query = task[0]
        golden_steps = task[1]
        if judge_type_task(golden_steps):
            type_task_num += 1
        function = ""
        if is_query_summary:
            function = task[2]
        print("##########################################")
        print(f"query:{query}")
        print(f"golden_steps:{golden_steps}")
        if is_random:
            answer_steps = random_interaction(page_dict)
        else:
            answer_steps = continuous_interaction(query, function, page_dict, page_summary, LLMmodel, is_query_summary,
                                                  is_page_summary, is_predictive, is_error_reply)
        # 任务结束后
        # 计时
        end_time = time.time()
        execution_time = end_time - start_time
        answer_steps_nums = len(answer_steps)
        if answer_steps_nums == 0:
            answer_steps_nums = 1
        instruct_average_time = execution_time / answer_steps_nums
        # 重置
        global HISTORY_OPT
        global PAGE_COOKIES
        HISTORY_OPT = []
        PAGE_COOKIES = []
        if evaluate.success(golden_steps, answer_steps):
            print("正确")
            correct += 1
            total_correct_cost_time += execution_time
            total_instruct_average_correct_cost_time += instruct_average_time
            if judge_type_task(golden_steps):
                correct_type_task_num += 1
        else:
            print("错误")
            total_wrong_cost_time += execution_time
            total_instruct_average_wrong_cost_time += instruct_average_time
        total_cost_time += execution_time
        total_instruct_average_cost_time += instruct_average_time
        type_instruct_num, select_instruct_num, click_instruct_num, human_instruct_num, type_instruct_correct_num, select_instruct_correct_num, click_instruct_correct_num, human_instruct_correct_num = evaluate.instruct_precision(
            golden_steps, answer_steps)

        type_instruct_total += type_instruct_num
        select_instruct_total += select_instruct_num
        click_instruct_total += click_instruct_num
        human_instruct_total += human_instruct_num
        type_instruct_correct_total += type_instruct_correct_num
        select_instruct_correct_total += select_instruct_correct_num
        click_instruct_correct_total += click_instruct_correct_num
        human_instruct_correct_total += human_instruct_correct_num

        print(f"-------------------------------")
        print(f"answer_steps:{answer_steps}")
        print(f"type:{type_instruct_correct_num}/{type_instruct_num}")
        print(f"select:{select_instruct_correct_num}/{select_instruct_num}")
        print(f"click:{click_instruct_correct_num}/{click_instruct_num}")
        print(f"human:{human_instruct_correct_num}/{human_instruct_num}")
        print(f"cost_time:{instruct_average_time}/{execution_time}/{answer_steps_nums}")

    acc = correct / task_num
    type_task_acc = correct_type_task_num / type_task_num

    print("-------------------------------------------------")
    print(f"Total tasks:{correct}/{task_num}")
    print(f"Accuracy: {acc}")
    print(f"Total type tasks:{correct_type_task_num}/{type_task_num}")
    print(f"Type Task acc: {type_task_acc}")
    t1 = total_instruct_average_cost_time / task_num
    t2 = total_cost_time / task_num
    t3 = 0
    t4 = 0
    if not correct == 0:
        t3 = total_instruct_average_correct_cost_time / correct
        t4 = total_instruct_average_correct_cost_time / correct
    t5 = total_instruct_average_wrong_cost_time / (task_num - correct)
    t6 = total_wrong_cost_time / (task_num - correct)
    print(f"Total Cost:{total_instruct_average_cost_time}/{total_cost_time}/{t1}/{t2}")
    print(f"Total Correct Cost:{total_instruct_average_correct_cost_time}/{total_correct_cost_time}/{t3}/{t4}")
    print(f"Total Wrong Cost:{total_instruct_average_wrong_cost_time}/{total_wrong_cost_time}/{t5}/{t6}")
    type_precision = type_instruct_correct_total / type_instruct_total
    select_precision = select_instruct_correct_total / select_instruct_total
    click_precision = click_instruct_correct_total / click_instruct_total
    human_precision = human_instruct_correct_total / human_instruct_total
    instruct_total = type_instruct_total + select_instruct_total + click_instruct_total + human_instruct_total
    correct_instruct_total = type_instruct_correct_total + select_instruct_correct_total + click_instruct_correct_total + human_instruct_correct_total
    total_precision = correct_instruct_total / instruct_total
    print(f"Total instruct:{correct_instruct_total}/{instruct_total}; Precision:{total_precision}")
    print(f"type instruct:{type_instruct_correct_total}/{type_instruct_total}; Precision:{type_precision}")
    print(f"select instruct:{select_instruct_correct_total}/{select_instruct_total}; Precision:{select_precision}")
    print(f"click instruct:{click_instruct_correct_total}/{click_instruct_total}; Precision:{click_precision}")
    print(f"human instruct:{human_instruct_correct_total}/{human_instruct_total}; Precision:{human_precision}")
    print("-------------------------------------------------")
    print(f"model:{model}")
    print(f"is_query_summary:{is_query_summary}")
    print(f"is_page_summary:{is_page_summary}")
    print(f"is_error_reply:{is_error_reply}")
    print(f"is_predictive:{is_predictive}")
    return correct_type_task_num, type_task_num, correct, task_num, type_precision, select_precision, click_precision, human_precision, instruct_total, correct_instruct_total, total_precision


def load_page_summary(page_dict, page_summary_path, LLMmodel, summary_word_num):  # 调用GPT生成页面摘要
    page_summary_dict = {}
    if os.path.exists(page_summary_path):
        with open(page_summary_path, 'r') as file:
            page_summary_dict = json.load(file)
    else:  # 未生成过页面摘要，需生成
        page_summary_prompt = f"""你是一个APP页面摘要生成器，要求如下：你会得到APP界面的页面名称和可操作的指令列表，根据上述信息做一个关于该页面功能的总结，字数少于%s字。""" % (
            summary_word_num)

        for page_name in page_dict.keys():
            page = page_dict[page_name]
            instruct_set_text = instruct_set2text(page.instruct_dict)
            observation_prompt = f"""当前页面名称: %s
            指令（指令名称，动作类型）: %s""" % (page_name, str(instruct_set_text))

            gpt_messages = [
                {"role": "system", "content": page_summary_prompt},
                {"role": "user", "content": observation_prompt}
            ]

            if LLMmodel in ['gpt-3.5-turbo', "gpt-3.5-turbo-1106", "gpt-4"]:
                msg = chat_to_GPT(gpt_messages, GPTmodel='gpt-4')
                LLM_reply = get_msg_content_gpt(msg)
            elif LLMmodel == "llama":
                llama_conversation_history = change_history_gpt2llama(gpt_messages)
                msg = chat_to_llama(llama_conversation_history)
                LLM_reply = get_msg_content_llama(msg)
            elif LLMmodel in ["qwen-turbo", "qwen1.5-72b-chat"]:
                msg = chat_to_qwen(gpt_messages, LLMmodel)
                LLM_reply = get_msg_content_qwen(msg)
            elif LLMmodel in BAICHUAN_MODELS:
                msg = chat_to_baichuan_openaiSDK(gpt_messages, LLMmodel)
                LLM_reply = get_msg_content_gpt(msg)

            page_summary = LLM_reply
            page_summary_dict[page_name] = page_summary
        with open(page_summary_path, 'w', encoding='utf-8') as file:
            json.dump(page_summary_dict, file, ensure_ascii=False)

    return page_summary_dict


def query_summary_tasks(query_summary_task_path, task_path, LLMmodel):
    # 调用GPT生成用户需求归纳
    if os.path.exists(query_summary_task_path):
        task_list_add_query = load_task_json(query_summary_task_path, is_query_summary=True)
    else:  # 未生成过用户需求归纳，需生成
        task_list = load_task_json(task_path)
        task_list_add_query = []
        for task in task_list:
            query = task[0]
            answer = task[1]
            func = query_summary(query, LLMmodel)
            task_list_add_query.append({"query": query, "function": func, "answer": answer})
        json_str = {"tasks": task_list_add_query}
        with open(query_summary_task_path, 'w', encoding='utf-8') as file:
            json.dump(json_str, file, ensure_ascii=False)
    return task_list_add_query


def query_summary(user_query, LLMmodel):
    # 调用GPT输出用户需求的功能
    query_summary_prompt = f"""你是一个用户需求意图推断生成器，你会被提供用户的需求，请拆解步骤，尽可能精简。
    提问：
    帮我用支付宝转账30给小李。
    回复：
    转账"""
    gpt_messages = [
        {"role": "system", "content": query_summary_prompt},
        {"role": "user", "content": user_query}
    ]

    if LLMmodel in ['gpt-3.5-turbo', "gpt-3.5-turbo-1106", "gpt-4.0"]:
        msg = chat_to_GPT(gpt_messages, LLMmodel)
        LLM_reply = get_msg_content_gpt(msg)
    elif LLMmodel == "llama":
        llama_conversation_history = change_history_gpt2llama(gpt_messages)
        msg = chat_to_llama(llama_conversation_history)
        LLM_reply = get_msg_content_llama(msg)
    elif LLMmodel in ["qwen-turbo", "qwen1.5-72b-chat"]:
        msg = chat_to_qwen(gpt_messages, LLMmodel)
        LLM_reply = get_msg_content_qwen(msg)
    elif LLMmodel in BAICHUAN_MODELS:
        msg = chat_to_baichuan_openaiSDK(gpt_messages, LLMmodel)
        LLM_reply = get_msg_content_gpt(msg)

    function = LLM_reply
    print(function)

    return function


def path_prepare(app_name):
    data_path = ""
    if app_name == "支付宝":
        data_path = "Alipay"
    elif app_name == "美团":
        data_path = "Meituan"
    elif app_name == "QQ邮箱":
        data_path = "QQMail"
    elif app_name == "QQ音乐":
        data_path = "QQMusic"
    elif app_name == "高德地图":
        data_path = "Amap"
    elif app_name == "高德地图2":
        data_path = "Amap2"
    elif app_name == "京东":
        data_path = "JD"
    elif app_name not in ["支付宝", "美团", "QQ邮箱", "QQ音乐", "高德地图", "高德地图2", "京东"]:
        print(f'ERROR: app {app_name} not found')
        return ""

    return data_path


def ask(query, page_folder_path, GPTmodel="gpt-4"):
    page_dict = load_data(page_folder_path)
    return continuous_interaction(query, "", page_dict, "", GPTmodel, False,
                                  False, False, True)


if __name__ == '__main__':

    result = {}
    # app_names = ["支付宝", "美团", "QQ邮箱", "QQ音乐"]
    # models = ['gpt-3.5', "gpt-4"]
    # for model in models:
    #     for app_name in app_names:
    # 路径准备
    # app_name = sys.argv[1]
    # model = sys.argv[2]
    # app_name ="京东"
    # model = "gpt-3.5-turbo-1106"

    parser = argparse.ArgumentParser(description='Process.')
    parser.add_argument('--model', help="LLMmodel", default="qwen1.5-72b-chat", dest='model')
    parser.add_argument('--dataset', help="dataset name", default="高德地图2", dest='dataset')
    parser.add_argument('--qs', help="is_query_summary", action="store_true", default=False, dest='is_query_summary')
    parser.add_argument('--ps', help="is_page_summary", action="store_true", default=False, dest='is_page_summary')
    parser.add_argument('--pred', help="is_predictive", action="store_true", default=False, dest='is_predictive')
    parser.add_argument('--er', help="is_error_reply", action="store_true", default=False, dest='is_error_reply')
    parser.add_argument('--rand', help="is_rand", action="store_true", default=False, dest='is_random')

    args = parser.parse_args()

    app_name = args.dataset
    model = args.model
    is_query_summary = args.is_query_summary
    is_page_summary = args.is_page_summary
    is_predictive = args.is_predictive
    is_error_reply = args.is_error_reply
    is_random = args.is_random

    data_path = path_prepare(app_name)
    if model == "gpt-4":
        file_model_name = "GPT4"
    elif model == 'gpt-3.5-turbo':
        model = "gpt-3.5-turbo"
        file_model_name = "GPT3.5"
    elif model == 'gpt-3.5-turbo-1106':
        model = "gpt-3.5-turbo-1106"
        file_model_name = "GPT3.5-1106"
    elif model == "llama":
        file_model_name = "LLAMA"
    elif model == "qwen-turbo":
        file_model_name = "QWEN_TURBO"
    elif model == "qwen1.5-72b-chat":
        file_model_name = "QWEN1.5_72B_CHAT"
    elif model in BAICHUAN_MODELS:
        file_model_name = model

    task_json_file = data_path + "/task/task.json"
    query_summary_task_path = data_path + "/summary/query_summary_task" + file_model_name + ".json"
    page_folder_path = "./" + data_path + "/pages"

    if model == "gpt-4":
        page_summary_path = data_path + "/summary/page_summary.json"
    elif model in ["gpt-3.5-turbo", "gpt-3.5-turbo-1106"]:
        page_summary_path = data_path + "/summary/page_summary.json"
    elif model == "llama":
        page_summary_path = data_path + "/summary/page_summary" + file_model_name + ".json"
    elif model in ["qwen-turbo", "qwen1.5-72b-chat"]:
        page_summary_path = data_path + "/summary/page_summary" + file_model_name + ".json"
    elif model in ["Baichuan2-turbo", "Baichuan4"]:
        page_summary_path = data_path + "/summary/page_summary" + file_model_name + ".json"

    print("-------------------------------------------------")
    print(f"app_name:{app_name}")
    print(f"model:{model}")
    print(f"is_query_summary:{is_query_summary}")
    print(f"is_page_summary:{is_page_summary}")
    print(f"is_error_reply:{is_error_reply}")
    print(f"is_predictive:{is_predictive}")
    print(f"is_random:{is_random}")
    # tasks = load_task_json(task_json_file)
    # batch_data(task_json_file, query_summary_task_path, page_summary_path, page_folder_path, model,
    #            is_query_summary=is_query_summary, is_page_summary=is_page_summary, is_predictive=is_predictive,
    #            is_error_reply=is_error_reply)

    # query = "我想把50块钱从支付宝转到银行卡里，卡号为123456，持卡人姓名为猪猪"
    # continuous_interaction(query)
    correct_type_task_num, type_task_num, correct, task_num, type_precision, \
    select_precision, click_precision, human_precision, instruct_total, \
    correct_instruct_total, total_precision = batch_data(task_json_file, query_summary_task_path, page_summary_path,
                                                         page_folder_path, model, is_query_summary=is_query_summary,
                                                         is_page_summary=is_page_summary, is_predictive=is_predictive,
                                                         is_error_reply=is_error_reply, is_random=is_random)
    # temp_result = {
    #     "acc": correct / task_num,
    #     "correct": correct,
    #     "task_num": task_num,
    #     "type_task_acc": correct_type_task_num / type_task_num,
    #     "correct_type_task_num": correct_type_task_num,
    #     "type_task_num": type_task_num,
    #     "type_precision": type_precision,
    #     "select_precision": select_precision,
    #     "click_precision": click_precision,
    #     "human_precision": human_precision,
    #     "instruct_total": instruct_total,
    #     "correct_instruct_total": instruct_total,
    #     "total_precision": total_precision}
    # result[model + "-" + app_name] = temp_result
    # for key in result:
    #     print(f'---------------{key}-------------')
    #     print(f"Total tasks:{result['correct']}/{result['task_num']}")
    #     print(f"Accuracy: {result['acc']}")
    #     print(f"Total type tasks:{result['correct_type_task_num']}/{result['type_task_num']}")
    #     print(f"Type Task Accuracy: {result['type_task_acc']}")
    #     print(
    #         f"Total instruct:{result['correct_instruct_total']}/{result['instruct_total']}; Precision:{result['total_precision']}")
    #     print(
    #         f"type instruct:{result['type_instruct_correct_total']}/{result['type_instruct_total']}; Precision:{result['type_precision']}")
    #     print(
    #         f"select instruct:{result['select_instruct_correct_total']}/{result['select_instruct_total']}; Precision:{result['select_precision']}")
    #     print(
    #         f"click instruct:{result['click_instruct_correct_total']}/{result['click_instruct_total']}; Precision:{result['click_precision']}")
    #     print(
    #         f"human instruct:{result['human_instruct_correct_total']}/{result['human_instruct_total']}; Precision:{result['human_precision']}")
