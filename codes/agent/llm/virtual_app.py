import json
import copy
import re

from load_page import load_data, PAGE_FOLDER_PATH
import json
from openai import OpenAI
import httpx
import random
import evaluate

HISTORY_OPT = []  # (page,instruct,action,parameter)
PAGE_COOKIES = []


def interaction(page, user_prompt,model):
    # 如果页面停留不变，不刷新状态
    if len(PAGE_COOKIES) == 0 or PAGE_COOKIES[-1] != page.page_name:
        page_status = generate_new_page_status(page)
        PAGE_COOKIES.append(page_status)
    page_status = PAGE_COOKIES[-1]

    optional_instruct_set = get_optional_instruct_set(page, page_status)
    optional_instruct_set_text = instruct_set2text(optional_instruct_set)

    role_prompt = f"""你是一个智能移动应用助手，可以在所提供的页面指令中进行选择，以达成用户的目标。"""
    page_prompt = f"""页面将以以下格式提供。
当前页面名称: %s" 
可用指令（指令名称，动作类型）: %s""" % (page_status[0], str(optional_instruct_set_text))
    instruct_rule_prompt = f"""请在可选指令中选择下一步操作，格式为(指令名称,动作类型,参数)
    参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段，例如(扫一扫, click, )"""
    sys_prompt = role_prompt + "\n" + page_prompt + "\n" + instruct_rule_prompt + "\n"

    print(sys_prompt)
    # msg = input(sys_prompt)
    gpt_messages = [
        {"role": "system", "content": sys_prompt},
        {"role": "user", "content": user_prompt}
    ]
    msg = chat_to_GPT(gpt_messages,model)
    error_flag, res, _ = verify_reply(msg, page)

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
    instruct_list = page_status[1]
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


def get_msg_content(msg):
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


def verify_reply(msg, page):
    error_flag = 0
    error_msg = ""
    input_string = get_msg_content(str(msg))
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
        parameter = res[2].strip()
    except IndexError:
        re_ERROR = f"""格式为(指令名称,动作类型,参数)。参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段，例如(扫一扫, click, )
                 请重试。"""
        print(re_ERROR)
        error_msg = re_ERROR
        error_flag = 1
    else:
        print(f"{instruct_name},{action},{parameter}")
        if not instruct_name in page.instruct_dict:
            instruct_ERROR = f"页面中不存在该指令，请重试。页面中的指令列表为{page.instruct_dict.keys()}"
            error_msg = instruct_ERROR
            print(instruct_ERROR)
            error_flag = 1
        elif not action == page.instruct_dict[instruct_name].action:
            action_ERROR = "该指令动作类型与指令不匹配，请重试。"
            error_msg = action_ERROR
            print(action_ERROR)
            error_flag = 1
        elif action == "type" and parameter == "":
            type_ERROR = "未输入内容，请重试。"
            error_msg = type_ERROR
            print(type_ERROR)
            error_flag = 1
    finally:
        if error_flag:
            return error_flag, (), error_msg
        else:
            return error_flag, (instruct_name, action, parameter), error_msg


def chat_to_GPT(gpt_message, GPTmodel="gpt-3.5-turbo"):
    client = OpenAI(
        base_url="https://oneapi.xty.app/v1",
        api_key="sk-HdzIjs2EjeghrJDD17DbCfEbC2A94dA98d815bD826Af936c",
        http_client=httpx.Client(
            base_url="https://oneapi.xty.app/v1",
            follow_redirects=True,
        ),
    )

    completion = client.chat.completions.create(
        model=GPTmodel,
        messages=gpt_message
    )
    # print(completion.choices[0].message)
    # pattern = r"content='(.*)', role"
    # reg1 = re.compile(pattern)
    # res = reg1.findall(str(completion.choices[0].message))
    # print(res[0])
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


def continuous_interaction(query,model):
    page_dict = load_data(PAGE_FOLDER_PATH)
    page_name = "首页"

    conversation_history = []

    role_prompt = f"""你是一个智能移动应用助手，可以在所提供的页面指令中进行选择，以达成用户的目标。"""
    page_rule_prompt = f"""页面将以以下格式提供。
    当前页面名称: 
    可用指令（指令名称，动作类型）: """
    instruct_rule_prompt = f"""请在可选指令中选择下一步操作，格式为(指令名称,动作类型,参数)
    参数指的是当指令动作类型为type（输入）时，需要填写的内容。若为其他click、human、select，参数不填写但保留参数字段。
    提问：
    当前页面名称: <页面名称> 
    可用指令（指令名称，动作类型）:<可用指令序列>
    回复：
    (扫一扫, click, )"""
    sys_prompt = role_prompt + "\n" + page_rule_prompt + "\n" + instruct_rule_prompt + "\n"

    conversation_history.append({"role": "system", "content": sys_prompt})
    counter = 1
    while counter < 10:
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
        observation_prompt = f"""
                当前页面名称: %s" 
                可用指令（指令名称，动作类型）: %s""" % (page_status[0], str(optional_instruct_set_text))
        user_prompt = query + "\n" + observation_prompt
        conversation_history.append({"role": "user", "content": user_prompt})

        # 传入gpt得到回复
        msg = chat_to_GPT(conversation_history,model)
        gpt_reply = get_msg_content(msg)
        conversation_history.append({"role": "assistant", "content": gpt_reply})

        # 验证是否有错误
        error_flag, res, error_msg = verify_reply(msg, page)
        #不提供错误信息
        if error_flag:
            conversation_history.append({"role": "user", "content": "请重试"})
            #conversation_history.append({"role": "user", "content": error_msg})

        # 页面状态更新及跳转
        next_page_name = page_name
        if not error_flag:
            instruct_name, action, parameter = res
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


def load_task_json(json_file_path):
    task_list = []
    with open(json_file_path, 'r') as file:
        tasks = json.load(file)
    for task in tasks['tasks']:
        query = task['query']
        answer = []
        for step in task['answer']:
            answer.append((step["pageName"], step["instruct"], step["action"], step["parameter"]))
        task_list.append((query, answer))
    return task_list


def batch_data(json_file_path,model):
    task_list = load_task_json(json_file_path)
    random.shuffle(task_list)
    correct = 0
    task_num = len(task_list)
    for task in task_list:
        query = task[0]
        golden_steps = task[1]
        print(f"query:{query}")
        print(f"golden_steps:{golden_steps}")
        answer_steps = continuous_interaction(query,model)
        # 任务结束后重置
        global HISTORY_OPT
        global PAGE_COOKIES
        HISTORY_OPT = []
        PAGE_COOKIES = []
        if evaluate.success(golden_steps, answer_steps):
            correct += 1
        print(f"query:{query}")
        print(f"golden_steps:{golden_steps}")
        print(f"answer_steps:{answer_steps}")
    acc = correct / task_num
    print(f"Accuracy: {acc}")


if __name__ == '__main__':
    task_json_file = "Alipay/task/task.json"
    tasks = load_task_json(task_json_file)
    batch_data(task_json_file,model='gpt-3.5-turbo')
    # query = "我想把50块钱从支付宝转到银行卡里，卡号为123456，持卡人姓名为猪猪"
    # continuous_interaction(query)
