import os
import json
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt

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



if __name__ == '__main__':
    task_json_file_path = "Alipay/task/old/human_tasks_output.json"
    task_list = load_task_json(task_json_file_path)
    print(count(task_list))







