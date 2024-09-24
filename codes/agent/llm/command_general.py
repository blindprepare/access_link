import os

DATASET_LIST = ["支付宝", "美团", "QQ邮箱", "QQ音乐","高德地图2","京东"]
MODEL_LIST = ["gpt-3.5-turbo","gpt-4","Baichuan4","qwen1.5-72b-chat"]

def general_cmd(dataset,model,para_list,output_para_string):


    is_query_summary, is_page_summary, is_predictive, is_error_reply = para_list
    para_string=""

    if is_query_summary:
        para_string += "--qs "
    if is_page_summary:
        para_string += "--ps "
    if is_predictive:
        para_string += "--pred "
    if is_error_reply:
        para_string += "--er "



    output_filename = dataset
    if model == "gpt-4":
        output_filename += "-gpt4"
    elif model == 'gpt-3.5-turbo':
        output_filename += "-gpt3.5"
    elif model == "qwen1.5-72b-chat":
        output_filename += "-qw72"
    elif model == "Baichuan4":
        output_filename += "-bc4"
    output_filename = output_filename +output_para_string+".txt"


    cmd = "python virtual_app_plus.py "
    cmd = cmd + "--dataset "+dataset + " --model "+model+" "+para_string+" >result/02/"+output_filename+" &\n"
    print(cmd)
    return cmd


def write_shell():
    is_query_summary = False
    is_page_summary = False
    is_predictive = False
    is_error_reply = True
    para_list = (is_query_summary,is_page_summary,is_predictive,is_error_reply)
    output_para_string = ""
    if not is_query_summary:
        output_para_string += "-qs"
    if not is_page_summary:
        output_para_string += "-ps"
    if not is_predictive:
        output_para_string += "-sc"
    if not is_error_reply:
        output_para_string += "-er"

    for dataset in DATASET_LIST:
        output_path = dataset + output_para_string + ".sh"
        wfile = open(output_path, "w", encoding="utf-8")
        for model in MODEL_LIST:
            cmd = general_cmd(dataset,model,para_list,output_para_string)
            wfile.write(cmd)
        wfile.close()

if __name__=='__main__':
    write_shell()









