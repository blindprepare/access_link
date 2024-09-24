import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('TkAgg')
def draw_together():
    fig, ax1 = plt.subplots()
    data = [["LLM-F",34.48,45.59],
            ["--qs --ps --self-cor --err", 34.83, 53.79],
            ["--qs --ps --self-cor", 34.83, 61.38],
            ["--qs --ps", 35.17, 54.14],
            ["--qs", 37.93, 56.55],
            ["Agent",  36.55 ,48.65]



            ]
    x_list = []
    y1_list=[]
    y2_list=[]
    for model in data:
        x_list.append(model[0])
        y1_list.append(model[1])
        y2_list.append(model[2])


    bar_width = 0.3

    # 计算每个柱子的位置
    x1 = range(len(x_list))
    x2 = [x + bar_width for x in x1]


    # 绘制四个柱子
    ax1.bar(x1, y1_list, width=bar_width,bottom=0,color='#002c53', label='GPT-3.5')
    #ax2 = ax1.twinx()
    ax1.bar(x2, y2_list, width=bar_width,bottom=0,color='#ffa510', label='GPT-4')

    ax1.plot(range(len(x_list)), y1_list, color='#0c84c6', marker='o', linestyle='-', linewidth=2, label='')
    ax1.plot(x2, y2_list, color='#f74d4d', marker='o', linestyle='-', linewidth=2, label='')

    # 添加标签和标题
    ax1.set_xlabel('model')
    ax1.set_ylabel('task complete rate' )
    #ax2.set_ylabel('instruct precision')
    ax1.set_xticks(range(len(x_list)))
    ax1.set_xticklabels(x_list, rotation=45, ha='right')

    #plt.title('Bar Chart with Four Categories')
    plt.xticks([x + bar_width for x in range(len(x_list))], x_list)


    # 添加图例
    plt.legend()

    # 显示图形
    plt.show()
if __name__=='__main__':
    draw_together()