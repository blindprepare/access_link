def success(golden_path, path):
    key_golden_steps = []
    # 关键步骤
    for step in golden_path:
        if step[2] == "type" or step[2] == "human" or step[2] == "select":
            key_golden_steps.append(tuple(step))
    for step in key_golden_steps:
        if step not in path:
            return False
    return True


def step_equal(golden_step, step):
    if golden_step[0] == step[0] and golden_step[1] == step[1] and golden_step[2] == step[2]:
        if golden_step[2] == "click" or golden_step[2] == "select" or golden_step[2] == "human":
            return True
        elif golden_step[3] == step[3]:
            return True
        elif golden_step[3] == "" and step[3] == "无":
            return True
    return False


def min_edit_distance(golden_path, path):
    len_golden_path = len(golden_path) + 1
    len_path = len(path) + 1

    # 创建一个二维数组来保存编辑距离
    distance_matrix = [[0] * len_path for _ in range(len_golden_path)]

    # 初始化第一行和第一列
    for i in range(len_golden_path):
        distance_matrix[i][0] = i
    for j in range(len_path):
        distance_matrix[0][j] = j

    # 计算编辑距离
    for i in range(1, len_golden_path):
        for j in range(1, len_path):
            cost = 0 if step_equal(golden_path[i - 1], path[j - 1]) else 1
            distance_matrix[i][j] = min(
                distance_matrix[i - 1][j] + 1,  # 删除
                distance_matrix[i][j - 1] + 1,  # 插入
                distance_matrix[i - 1][j - 1] + cost  # 替换
            )

    # 最终编辑距离保存在右下角
    return distance_matrix[len_golden_path - 1][len_path - 1]


def instruct_precision(golden_path, answer_path):
    type_instruct_num = 0
    select_instruct_num = 0
    click_instruct_num = 0
    human_instruct_num = 0
    type_instruct_correct_num = 0
    select_instruct_correct_num = 0
    click_instruct_correct_num = 0
    human_instruct_correct_num = 0
    for step in golden_path:
        if step[2] == "type":
            type_instruct_num += 1
        elif step[2] == "select":
            select_instruct_num += 1
        elif step[2] == "human":
            human_instruct_num += 1
        elif step[2] == "click":
            click_instruct_num += 1
    for ans_step in answer_path:
        for golden_step in golden_path:
            if step_equal(golden_step, ans_step):
                if ans_step[2] == "type":
                    type_instruct_correct_num += 1
                elif ans_step[2] == "select":
                    select_instruct_correct_num += 1
                elif ans_step[2] == "human":
                    human_instruct_correct_num += 1
                elif ans_step[2] == "click":
                    click_instruct_correct_num += 1
                golden_path.remove(golden_step)
    return type_instruct_num, select_instruct_num, click_instruct_num, human_instruct_num, type_instruct_correct_num, select_instruct_correct_num, click_instruct_correct_num, human_instruct_correct_num
