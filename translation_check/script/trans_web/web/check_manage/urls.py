from django.urls import path

from . import views

urlpatterns = [

    # ex: /checkManage/getModuleNames
    path("getModuleNames/", views.getModuleNames, name="getModuleNames"),
    # ex: /checkManage/getQuestionTypes
    path("getQuestionTypes/", views.getQuestionTypes, name="getQuestionTypes"),
    # ex: /checkManage/searchCheckInfo/
    path("searchCheckInfo/<str:taskType>", views.searchCheckInfo, name="searchCheckInfo"),

]