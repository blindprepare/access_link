import json
import copy
import re
import statistics

from load_page import load_data, PAGE_FOLDER_PATH

from openai import OpenAI
import httpx
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt

OPENAI_API_KEY = "***************************************"

NO_CIRCLE_PATH = []
CIRCLE_PATH = []
NO_COMPLETE_PATH = []

user_query_prompt = "假设你是用户需要使用这个功能，请以用户口吻提出这个需求，使提问对应的问题答案是给定的操作路径。"
sys_prompt = f"""你是一个app产品功能助手，给定app和操作路径，使用一个词组，归纳完整路径的意图功能。
请你考虑到过程中途径的操作和页面名称，捕获特征信息，使用最少的字数。操作路径的格式是(页面名称,操作名称,动作类型,参数)。
以下是一个完整的例子：
app: QQ音乐
path: [('首页', '点击搜索框', 'click', ''), ('搜索页面', '听书热榜', 'click', ''), ('听书热榜页面', '点击飙升榜', 'click', ''), ('听书飙升榜页面', '选择飙升书目', 'select', ''), ('飙升书目详情页面', '播放书目', 'human', '')]
飙升榜中选择书目听书"""

sys_prompt_simple = f"""你是一个app产品功能助手，给定app和操作路径，使用一个词组，归纳完整路径的意图功能。
请你考虑到过程中途径的操作和页面名称，捕获特征信息，使用最少的字数。操作路径的格式是(页面名称,操作名称,动作类型,参数)。
以下是一个完整的例子：
app: 支付宝
path: [('首页', '扫一扫', 'click', ''), ('扫一扫页面', '扫码', 'human', '')]
扫一扫"""

sys_prompt_0 = f"""你是一个app产品功能助手，给定app和操作路径，使用一个词组，归纳完整路径的意图功能。
请你考虑到过程中途径的操作和页面名称，捕获特征信息，使用最少的字数。操作路径的格式是(页面名称,操作名称,动作类型,参数)。"""



sys_prompt_2 = f"""你是一个app产品的使用者，给定app、操作路径和意图功能，请以用户口吻提出需求，使提问对应的问题答案是给定的操作路径，参考意图功能。操作路径的格式是(页面名称,操作名称,动作类型,参数)。
请生成三条用户需求，用;分割。
以下是一个完整的例子：
提问：
app: QQ音乐
path: [('首页', '点击搜索框', 'click', ''), ('搜索页面', '听书热榜', 'click', ''), ('听书热榜页面', '点击飙升榜', 'click', ''), ('听书飙升榜页面', '选择飙升书目', 'select', ''), ('飙升书目详情页面', '播放书目', 'human', '')]
intention:飙升榜中选择书目听书
回复：
我想打开飙升榜页面找书听;我想听书，请帮我打开书目飙升榜;打开飙升榜，我要选书目听书
"""
sys_prompt_3 = f"""你是一个app产品的使用者，给定app、操作路径和意图功能，请以用户口吻提出需求，使提问对应的问题答案是给定的操作路径，参考意图功能。操作路径的格式是(页面名称,操作名称,动作类型,参数)。
请生成三条用户需求，用;分割。
"""

def prompt_creator(app_name,path):
    query_prompt = f"""app: {app_name}
                       path: {path}
                       """

    return query_prompt

def prompt_creator_func(app_name,path,functiom):
    query_prompt = f"""app: {app_name}
                       path: {path}
                       intention: {functiom}
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

    def func_obj2json(self, func, page_dict):
        ans_list = []
        for item in self.answer:
            ans_list.append({"pageName": item.page_name, "instruct": item.instruct, "action": item.action, "parameter": ""})
        res_str = {"query": self.query,"function":func, "answer": ans_list }
        return res_str

def load_raw_scene(json_file_path):
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


def load_raw_function(json_file_path):
    task_list = []
    with open(json_file_path, 'r') as file:
        tasks = json.load(file)
    for task in tasks['tasks']:
        query = task['query']
        function = task['function']
        answer = []
        for step in task['answer']:
            answer.append((step["pageName"], step["instruct"], step["action"], step["parameter"]))
        task_list.append((query, answer, function))
    return task_list

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

def write_json(path_list,json_path,page_dict):
    tasks = []
    for path in path_list:
        tmp_list = []
        for step in path:
            tmp_list.append(STEP(step[0], step[1], step[2]))
        for i in range(0,1):
            #query = input(path)
            query = ""
            new_task = TASK(query, tmp_list)
            new_task_dict = new_task.obj2json(page_dict)
            tasks.append(new_task_dict)
    json_str = {"tasks": tasks}
    print(json_str)

    with open(json_path, 'w', encoding='utf-8') as file:
        json.dump(json_str, file ,ensure_ascii=False)

def write_function_json(raw_task_list,json_path,page_dict):
    tasks = []
    for task in raw_task_list:
        tmp_list = []
        query,function,path =task
        for step in path:
            tmp_list.append(STEP(step[0], step[1], step[2]))
        for i in range(0,1):
            #query = input(path)
            #query = ""
            new_task = TASK(query, tmp_list)
            new_task_dict = new_task.func_obj2json(function,page_dict)
            tasks.append(new_task_dict)
    json_str = {"tasks": tasks}
    print(json_str)

    with open(json_path, 'w', encoding='utf-8') as file:
        json.dump(json_str, file ,ensure_ascii=False)


def write_task_json(raw_task_list,json_path,page_dict):
    tasks = []
    for task in raw_task_list:
        tmp_list = []
        query,function,path =task
        for step in path:
            tmp_list.append(STEP(step[0], step[1], step[2]))
        for i in range(0,1):
            #query = input(path)
            new_task = TASK(query, tmp_list)
            new_task_dict = new_task.func_obj2json(function,page_dict)
            tasks.append(new_task_dict)
    json_str = {"tasks": tasks}
    print(json_str)

    with open(json_path, 'w', encoding='utf-8') as file:
        json.dump(json_str, file ,ensure_ascii=False)

def chat_to_GPT(sys_prompt, user_prompt, GPTmodel="gpt-4"):
    client = OpenAI(
        base_url="https://oneapi.xty.app/v1",
        api_key=OPENAI_API_KEY,
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
    # print(completion.choices[0].message)
    pattern = r"content='(.*)', role"
    reg1 = re.compile(pattern)
    res = reg1.findall(str(completion.choices[0].message))
    print(res[0])
    return res[0]

    # query_pattern = r"content='(.*)1: (.*)\\n.*2: (.*)\\n.*3:(.*)', role="
    # reg2 = re.compile(query_pattern)
    # query_res = reg2.findall(str(completion.choices[0].message).strip())
    # print(query_res)
    # if query_res == []:
    #     return []
    # else:
    #     return query_res[0][1:4]


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


def human_select(path_list):
    selected_path = []
    for path in path_list:
        ans = input(path)
        if ans == "y":
            selected_path.append(path)
        elif ans == "n":
            continue

    return selected_path

def draw(scene_list,app_name):
    count_type = 0
    count_answer_length = {}
    for scene in scene_list:
        query, answer = scene

        #计算各个任务的指令长度
        length = len(answer)
        if count_answer_length.__contains__(length):
            count_answer_length[length] += 1
        else:
            count_answer_length[length] = 1
    #绘图
    x_min = 1
    x_max = max(count_answer_length.keys())
    x_list = [i for i in range(1, x_max+1)]
    y_list = []
    for x in x_list:
        if count_answer_length.__contains__(x):
            y_list.append(count_answer_length[x])
        else:
            y_list.append(0)
    print(x_list)
    print(y_list)

    # 创建柱状图
    plt.bar(x_list, y_list)

    # 添加标题和标签
    plt.title(app_name)
    plt.ylabel('length of instructs ')
    plt.xlabel('numbers of scenarios')

    # 显示图形
    plt.show()

    return len(scene_list), count_type
def draw_together():
    x_list = [1, 2, 3, 4, 5, 6, 7, 8]
    Alipay_y_list = [0, 2, 16, 13, 2, 3, 3, 1]
    Meituan_y_list = [0, 3, 4, 1, 2, 1, 0, 0]
    QQMail_y_list = [0, 0, 10, 1, 2, 1, 0, 0]
    QQMusic_y_list = [0, 2, 9, 11, 5, 0, 0, 0]
    Amap_y_list = [0,0,4,0,10,7,5,2]
    JD_y_list=[0,5,17,3,2,2,0,0]
    all = []
    for i in range(0,len(x_list)):
        num = Alipay_y_list[i]+Meituan_y_list[i]+QQMusic_y_list[i]+QQMail_y_list[i]+Amap_y_list[i]+JD_y_list[i]
        all +=[i+1]*num
    print("mean:"+str(statistics.mean(all)))
    print("median:"+str(statistics.median(all)))

    bar_width = 0.2

    # 计算每个柱子的位置
    x1 = range(len(x_list))
    x2 = [x + bar_width for x in x1]
    x3 = [x + bar_width for x in x2]
    x4 = [x + bar_width for x in x3]
    x5 = [x + bar_width for x in x4]
    x6 = [x + bar_width for x in x5]


    # 绘制四个柱子
    plt.bar(x1, Alipay_y_list, width=bar_width, label='Alipay')
    plt.bar(x2, Meituan_y_list, width=bar_width, label='Meituan')
    plt.bar(x3, QQMail_y_list, width=bar_width, label='QQMail')
    plt.bar(x4, QQMusic_y_list, width=bar_width, label='QQMusic')
    plt.bar(x5, Amap_y_list, width=bar_width, label='Amap')
    plt.bar(x6, JD_y_list, width=bar_width, label='JD')

    # 添加标签和标题
    plt.xlabel('length of instructs ')
    plt.ylabel('numbers of scenarios')
    #plt.title('Bar Chart with Four Categories')
    plt.xticks([x + bar_width for x in range(len(x_list))], x_list)


    # 添加图例
    plt.legend()

    # 显示图形
    plt.show()


def path_prepare(app_name):
    data_path =""
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
    elif app_name not in ["支付宝","美团","QQ邮箱","QQ音乐","高德地图","京东"]:
        print("ERROR: app not found")
        return "", "", "", ""
    return data_path

def best_path(page_dict,path):
    no_chose_page = []
    best_flag = True
    for step in path:
        page_name = step[0]
        instruct_name = step[1]
        action = step[2]
        page = page_dict[page_name]
        type_page_flag = False
        if page_name not in no_chose_page:
            no_chose_page.append(page_name)
        for iter_instruct in page.instruct_dict.keys():
            if page.instruct_dict[iter_instruct].action == "type":
                type_page_flag = True
        if type_page_flag:
            continue
        else:
            if action != "human" and page.instruct_dict[instruct_name].next_page_name in no_chose_page:
                best_flag = False
                break
            for iter_instruct in page.instruct_dict.keys():
                if iter_instruct not in no_chose_page:
                    no_chose_page.append(page.instruct_dict[iter_instruct].next_page_name)

    return best_flag





if __name__ == '__main__':
    all_path = []
    # 路径准备
    app_name = "京东"
    data_path = path_prepare(app_name)
    model = 'gpt-3.5'

    if model == "gpt-4":
        file_model_name = "GPT4"
    elif model == 'gpt-3.5':
        model = "gpt-3.5-turbo"
        file_model_name = "GPT3.5"

    task_json_file = data_path + "/task/task.json"
    query_summary_task_path = data_path + "/summary/query_summary_task.json"
    page_summary_path = data_path + "/summary/page_summary.json"
    page_folder_path = "./" + data_path + "/pages"
    raw_scene_path = data_path + "/task/raw_scene.json"
    raw_func_path = data_path + "/task/raw_function_"+file_model_name+".json"
    human_raw_func_path = data_path + "/task/raw_function.json"
    raw_task_path = data_path + "/task/raw_task_"+file_model_name+".json"


    page_dict = load_data(page_folder_path)
    ###########################
    # 统计页面数和指令数
    page_count = 0
    instruct_count = 0
    page_count = len(page_dict.keys())
    for page_name in page_dict.keys():
        instruct_count += len(page_dict[page_name].instruct_dict.keys())
    print("page_count: "+str(page_count))
    print("instruct_count: "+str(instruct_count))
    # ############################
    #
    depth_first_traversal(page_dict, "首页", all_path)


    #1、人工筛选场景
    print("no_selected_path:"+str(len(NO_CIRCLE_PATH)))

    first_selected_path = []
    ready_human_selected_path = []
    for path in NO_CIRCLE_PATH:
        if best_path(page_dict,path):
            first_selected_path.append(path)
        else:
            ready_human_selected_path.append(path)
    print("first_selected_path:" + str(len(first_selected_path)))
    for path in first_selected_path:
        print(path)
    #human_selected_path = human_select(ready_human_selected_path)
    human_selected_path = []
    selected_path = human_selected_path + first_selected_path
    write_json(selected_path, raw_scene_path, page_dict)
    # ########################
    #
    selected_path = load_raw_scene(raw_scene_path)
    # for path in selected_path:
    #     print(path)

    ###########################
    # # 2、场景计数（需完成1）
    # type_scene_count = 0
    # for scene in selected_path:
    #     path = scene[1]
    #     for step in path:
    #         if step[2] == "type":
    #             type_scene_count += 1
    #             continue
    # print("type_scene_count:"+str(type_scene_count))
    # print("scene_count:"+str(len(selected_path)))
    #############################
    #############################
    # 3-1、画图（单个应用）
    #draw(selected_path,app_name=data_path)
    # 3-2、画图（所有应用）
    #draw_together()

    ############################
    # 4、场景生成功能（需完成1）
    raw_func_list = []
    for path in selected_path:
        query = ""

        user_prompt = prompt_creator(app_name, path[1])
        #print(sys_prompt)
        #print(user_prompt)
        print(path[1])
        function=chat_to_GPT(sys_prompt_simple,user_prompt,GPTmodel=model)
        raw_func_list.append((query,function,path[1]))
    write_function_json(raw_func_list,raw_func_path, page_dict)
    ##########################

    # ############################
    # # 5、功能生成需求（需完成4后经过人工，将修改后的文件保存在raw_function.json）
    # task_list = load_raw_function(human_raw_func_path)
    # auto_task_list = []
    # for task in task_list:
    #     query_list = []
    #     function = task[2]
    #     user_prompt = prompt_creator_func(app_name, task[1], task[2])
    #     # print(sys_prompt)
    #     # print(user_prompt)
    #     print(task[1])
    #     print(task[2])
    #     res = chat_to_GPT(sys_prompt_3, user_prompt, GPTmodel=model)
    #     query_list = res.split(';')
    #     for query in query_list:
    #         print(query)
    #         auto_task_list.append((query, function, task[1]))
    # print(auto_task_list)
    # write_function_json(auto_task_list, raw_task_path, page_dict)
    # ############################
    #write_json(NO_CIRCLE_PATH,page_dict)

    task_list = load_raw_function(task_json_file)
    # ############################
    # 任务计数
    type_task_count = 0
    for scene in task_list:
        path = scene[1]
        for step in path:
            if step[2] == "type":
                type_task_count += 1
                break
    print("type_task_count:"+str(type_task_count))
    print("task_count:"+str(len(task_list)))
    # ############################
