import json
import copy
import re

from load_page import load_data, PAGE_FOLDER_PATH

from openai import OpenAI
import httpx

OPENAI_API_KEY = "***************************************"

NO_CIRCLE_PATH = []
CIRCLE_PATH = []
NO_COMPLETE_PATH = []

sys_prompt = f"""你是一个app智能助手，给定app和操作路径，请你归纳路径对应的功能，并以用户的口吻对这个功能做三个提问，使提问对应的问题答案是给定的操作路径。以下是一个完整的例子：
                           app: 支付宝
                           path: [('首页', '扫一扫', 'click'), ('扫一扫页面', '扫码', 'human')]
                           function: 扫码" 
                           query1: 请使用支付宝扫码
                           query2: 我想用支付宝扫一下码
                           query3: 我想用支付宝扫一下这个二维码"""


def prompt_creator(path):
    query_prompt = f"""app: 支付宝
                       path: {path}
                       function: 
                       query1: 
                       query2: 
                       query3: 
                       """

    return query_prompt


class STEP():
    def __init__(self, page_name, instruct, action):
        self.page_name = page_name
        self.instruct = instruct
        self.action = action


class TASK():
    def __init__(self, query, list):
        self.query = query
        self.answer = list

    def obj2json(self, page_dict):
        ans_list = []
        for item in self.answer:
            ans_list.append({"pageName": item.page_name, "instruct": item.instruct, "action": item.action, "parameter": ""})
        res_str = {"query": self.query, "answer": ans_list }
        return res_str


def write_auto_json(path_list):
    tasks = []
    for path in path_list:
        user_prompt = prompt_creator(path)
        query_list = chat_to_GPT(sys_prompt, user_prompt)
        if query_list != []:
            for query in query_list:
                list = []
                for step in path:
                    list.append(STEP(step[0], step[1], step[2]))
                new_task = TASK(query, list)
                new_task_dict = new_task.obj2json
                tasks.append(new_task_dict)
    json_str = {"tasks": tasks}
    print(json_str)

    with open('Alipay/task/old/auto_tasks_output.json', 'w', encoding='utf-8') as file:
        json.dump(json_str, file,ensure_ascii=False)

def write_json(path_list,page_dict):
    tasks = []
    for path in path_list:
        tmp_list = []
        for step in path:
            tmp_list.append(STEP(step[0], step[1], step[2]))
        for i in range(0,3):
            query = input(path)
            new_task = TASK(query, tmp_list)
            new_task_dict = new_task.obj2json(page_dict)
            tasks.append(new_task_dict)
    json_str = {"tasks": tasks}
    print(json_str)

    with open('Alipay/task/old/human_tasks_output.json', 'w', encoding='utf-8') as file:
        json.dump(json_str, file,ensure_ascii=False)

def chat_to_GPT(sys_prompt, user_prompt, GPTmodel="gpt-3.5-turbo"):
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
        messages=[
            {"role": "system", "content": sys_prompt},
            {"role": "user", "content": user_prompt}
        ]
    )
    print(completion.choices[0].message)
    # pattern = r"content='(.*)', role"
    # reg1 = re.compile(pattern)
    # res = reg1.findall(str(completion.choices[0].message))
    # print(res[0])

    query_pattern = r"content='(.*)1: (.*)\\n.*2: (.*)\\n.*3:(.*)', role="
    reg2 = re.compile(query_pattern)
    query_res = reg2.findall(str(completion.choices[0].message).strip())
    print(query_res)
    if query_res == []:
        return []
    else:
        return query_res[0][1:4]


def depth_first_traversal(page_dict, page_name, path=[]):
    # 获取当前页面的指令
    page = page_dict[page_name]
    instruct_list = page.instruct_dict.keys()
    # 遍历当前页面的指令
    for instruct in instruct_list:
        if instruct == "返回":
            continue
        # 将当前页面名称和指令添加到路径里
        action = page.instruct_dict[instruct].action
        path.append((page_name, instruct, action))
        # 如果是最后一步，将路径输出，输出后打印路径，并弹出最后一项
        if action == "human":
            NO_CIRCLE_PATH.append(copy.copy(path))
            path.pop()
        else:
            next_page_name = page.instruct_dict[instruct].next_page_name
            if not action == "type":
                if next_page_name == page_name:
                    for instruct_1 in instruct_list:
                        action_1 = page.instruct_dict[instruct_1].action
                        if action_1 == "human":
                            path.append((page_name, instruct_1, action_1))
                            NO_CIRCLE_PATH.append(copy.copy(path))
                            path.pop()
                            path.pop()
                            break
            circle_flag = False
            page_path = [""]
            for item in path:
                if item[0] != page_path[-1]:
                    page_path.append(item[0])
            if next_page_name in page_path and next_page_name != page_path[-1]:
                circle_flag = True
                CIRCLE_PATH.append(copy.copy(path))
                path.pop()
            # for item in path:
            #     if item[0] == next_page_name:
            #         circle_flag = True
            #         CIRCLE_PATH.append(copy.copy(path))
            #         path.pop()
            #         break
            if not circle_flag and next_page_name != page_name:
                # 判断将要访问的页面是否存在
                if not page_dict.__contains__(next_page_name):
                    NO_COMPLETE_PATH.append(copy.copy(path))
                    path.pop()
                else:
                    depth_first_traversal(page_dict, next_page_name, path)
                    path.pop()
    while path != [] and page_name == path[-1][0]:
        path.pop()


if __name__ == '__main__':
    all_path = []
    page_dict = load_data(PAGE_FOLDER_PATH)
    depth_first_traversal(page_dict, "首页", all_path)
    # for item in CIRCLE_PATH:
    #     print(item)
    #
    # print('\n')
    for item in NO_CIRCLE_PATH:
        print(item)

    print(len(NO_CIRCLE_PATH))
    # user_prompt = prompt_creator(NO_CIRCLE_PATH[1])
    # print(sys_prompt)
    # print(user_prompt)
    # chat_to_GPT(sys_prompt,user_prompt)

    write_json(NO_CIRCLE_PATH,page_dict)
