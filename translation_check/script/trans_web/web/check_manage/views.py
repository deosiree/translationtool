from django.shortcuts import render

# Create your views here.
from . import *
from .script import service as Service
from django.http import HttpResponse,HttpRequest,JsonResponse


def getModuleNames(request):
    """
        获取所有的功能模块的名称
    """
    dataDict = {
        "code" : 200,
        "message": "success",
        "data" : {
            "totalNum" : len(MODULE_NAMES),
            "list" : MODULE_NAMES
        }
    }
    response = JsonResponse(dataDict)

    return response

def getQuestionTypes(requrest):
    """
        查询数据库中所有问题的类型

        不同问题对应的模块可能相同
        一个模块对应多个不同的类型
    """

    dataDict = {
        "code" : 200,
        "message": "success",
        "data" : {
            "totalNum" : len(QUESTION_TYPE),
            "list" : QUESTION_TYPE
        }
    }
    response = JsonResponse(dataDict)
    return response




def searchCheckInfo(request : HttpRequest,taskType : str):
    # requestBodyDict = json.loads(request.body)
    # paramsDict : dict = requestBodyDict["params"]

    try:
        if taskType in TASK_TYPES:
            if taskType == "code":
                data = Service.checkNamespace(request)
        else:
            raise Exception("当前任务类型不支持，当前支持的任务类型为{}".format(TASK_TYPES))
        code = 200
        message = "success"
    except Exception as e:
        code = 205
        message = str(e)

    responseBody = {
        "code": code,
        "message": message
    }
    if code == 205:
        responseBody["data"] = {"totalNum": 0,"list": []}
    else:
        responseBody["data"] = data
    response = JsonResponse(responseBody)
    return response


def detail(request, question_id):
    return HttpResponse("You're looking at question %s." % question_id)


def results(request, question_id):
    response = "You're looking at the results of question %s."
    return HttpResponse(response % question_id)


def vote(request, question_id):
    return HttpResponse("You're voting on question %s." % question_id)