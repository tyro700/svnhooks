1.svn服务器需要有jdk
2.需要配置好reviewboard，在config.properties下输入review站点的地址、用户名以及密码

3.将hook.jar放到svn hooks目录下
4.启用pre-commit脚本，脚本的内容参照文件pre-commit（如果是windows服务器则需要启用相应的.bat文件）
5.config.properties配置说明
    svn_experts ： 专家级用户，不需要审核可以直接提交代码的svn用户
    default_reviewers ： 默认审核员
    min_review_id ： 最小的有效review id
    min_shit_it_count ： 最少需要审核员通过的数量
    min_expert_ship_it_count ： 最少需要专家级审核员通过的数量
    min_review_id ： 有效的revie id，默认为1
    review_path ： 需要review的代码路径（包含于，多个路径用逗号隔开）
    ignore_path ： 忽略审核的代码路径