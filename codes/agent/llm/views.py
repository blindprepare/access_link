import json

from django.shortcuts import render

# Create your views here.
from django.http import HttpResponse

import llm
from llm.virtual_app_plus import path_prepare


def ask(request):
    query = request.GET.get("query")
    app_name = request.GET.get("appName")
    data_path = path_prepare(app_name)
    page_folder_path = "llm/" + data_path + "/pages"
    request_mapper = load('llm/requestHistory.json')
    if request_mapper.__contains__(query):
        return HttpResponse(json.dumps(request_mapper[query], ensure_ascii=False))
    mapper = load('llm/instructionMapper.json')
    answer_steps = llm.virtual_app_plus.ask(query, page_folder_path)
    instructions = []
    for step in answer_steps:
        flag = app_name + "_" + step[0] + "_" + step[1] + "_" + step[2]
        instruction_info = mapper[flag]
        instruction = {'page': instruction_info["page"], 'instruction': step[1], 'type': step[2],
                       "path": instruction_info["path"], "remark": step[3], "package": instruction_info["package"]}
        instructions.append(instruction)
    request_mapper[query] = instructions
    write('llm/requestHistory.json', request_mapper)
    return HttpResponse(json.dumps(instructions, ensure_ascii=False))


def load(path):
    with open(path, 'r') as file:
        return json.load(file)


def write(path, data):
    with open(path, "w") as dump_f:
        json.dump(data, dump_f)
