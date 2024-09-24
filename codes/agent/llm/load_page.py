import os
import json
from interface import App, Page, Instruct

PAGE_FOLDER_PATH = "./Alipay/pages"
APP_PATH = "./Alipay"
COUNT = 0

# 获取文件夹下所有文件名
def load_file_list(folder_path):
    filepath_list = []
    file_names = os.listdir(folder_path)
    # 打印所有文件名
    for file_name in file_names:
        # 获取文件的完整路径
        file_path = os.path.join(folder_path, file_name)
        filepath_list.append(file_path)
    return filepath_list


def load_page_json(file_path):
    page_name = ""
    # 从文件中读取 JSON 数据
    with open(file_path, 'r', encoding='utf-8') as file:
        file_content = file.read()
    try:
        # 尝试解析 JSON 字符串
        page_data = json.loads(file_content)
        page_name = page_data['pageName']
        new_page = Page(page_name)
        instruct_list = list(page_data['actionList'])
        for instruct in instruct_list:
            action = ""
            object_info = ""
            para = ""
            next_page_name = ""

            if instruct['type'] == "点击动作":
                action = "click"
            elif instruct['type'] == "输入动作":
                action = "type"
            elif instruct['type'] == "筛选动作":
                action = "select"
            elif instruct['type'] == "人为动作":
                action = "human"

            object_info = instruct['action']

            if instruct['parameter'] == "无需参数":
                para = ""
            else:
                para = instruct['parameter']

            next_page_name = instruct['nextPage']

            if not new_page.instruct_dict.__contains__(object_info):
                new_page.instruct_dict[object_info] = Instruct(action, object_info, para, next_page_name)
            else:
                print("Warning: {} has already exist object_info:{} ".format(new_page.page_name, object_info))
        return new_page
    except json.JSONDecodeError as e:
        print(f"无法解析 JSON：{e}")


def load_data(folder_path):
    count = 0
    json_list = load_file_list(folder_path)
    page_dict = {}
    for json_file in json_list:
        page = load_page_json(json_file)
        if not page_dict.__contains__(page.page_name):
            page_dict[page.page_name] = page
        else:
            print("Warning: Already exist page:{} ".format(page.page_name))
    return page_dict

def summary_data(page_dict):
    page_count = len(page_dict)
    instruct_count = 0
    #TODO
    return page_count,instruct_count
if __name__ == '__main__':
    # page_list = load_filelist(PAGE_FOLDER_PATH)
    COUNT = 0
    load_data(PAGE_FOLDER_PATH)
