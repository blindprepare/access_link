#!/usr/bin/python
# -*- coding: UTF-8 -*-
import openai
import prompt_library
INSTRUCT_ACTIONS = ["click", "type", "select","human"]
OPENAI_API_KEY="***************************************"
page_index = 0
role_describe_prompt = "You are an intelligent mobile APP assistant that can help users choose right intruct to achieve the aim." \
                "You are given an observation of the current APP page, in the following format:" \
                "Current Observation:" \
                "Optional Instruct:"
# response = openai.Completion.create(
#     engine="davinci",
#     prompt="Translate the following English text to French: 'Hello, how are you?'",
#     max_tokens=50
# )
#
# generated_text = response.choices[0].text
# print(generated_text)
class App:
    def __init__(self,app_name,app_index):
        self.app_name = app_name
        self.app_index = app_index
        self.page_list = ""

class Page:
    def __init__(self,page_name):
        self.page_name = page_name
        self.instruct_dict = {}
        self.page_intro = ""

    def generate_page_intro(self, words_limits, LLMengine):
        instruct_list_describe = ""
        for instruct in range(0, self.instruct_dict):
            instruct_list_describe += "Instruct %d : %s < %s > %s \n"
        page_intro_prompt="You are given an observation of the current APP page. " \
                          "According to the given observation, generate a summary of this page, " \
                          "less than %s words. " \
                          "The given observation is in the following format:" \
                          "page name: %s" \
                          "optional intruct: %s"%(words_limits,self.page_name,instruct_list_describe)
        if LLMengine in ["davinvi"]:
            response = openai.Completion.create(
                engine="davinci",
                prompt=page_intro_prompt,
                max_tokens=50
            )
            self.page_intro = generated_text = response.choices[0].text

class Instruct:
    def __init__(self,action,object_info,para,next_page_name):
        if action not in INSTRUCT_ACTIONS:
            raise ValueError("%s is not a defined intruct action.")
        else:
            self.action = action
        self.object_info = object_info
        self.para = para
        self.description = ""#TODO
        self.next_page_name = next_page_name


if __name__ == '__main__':
    openai.api_key = OPENAI_API_KEY
    LLMmodel = "gpt-3.5-turbo"

    messages = []
    system_message = prompt_library.SYS_MSG_SUMMARY
    system_message_dict = {
        "role": "system",
        "content": system_message
    }
    messages.append(system_message_dict)
    message = input("输入想要询问的信息: ")
    user_message_dict = {
        "role": "user",
        "content": message
    }
    messages.append(user_message_dict)
    print(messages)
    response=openai.ChatCompletion.create(
      model="gpt-3.5-turbo",
      messages=messages
    )
    print(response)
    reply = response["choices"][0]["message"]["content"]
    print(reply)




