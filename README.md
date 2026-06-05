# 社区养老服务系统

## 项目介绍

本项目是一个社区养老服务系统，包含后端服务和管理端前端，面向老人信息管理、家属绑定审核、服务预约、健康记录、紧急求助、消息通知和后台治理等业务场景。

## 技术栈

- Spring Boot 后端
- MySQL
- Maven
- Vue 3
- TypeScript
- Vite
- Element Plus
- Vue Router

## 部署要求

- JDK 17 或项目要求版本
- Maven 3.x
- MySQL 8.0
- Node.js 18 或以上
- npm/pnpm

## 运行流程

1. 创建数据库并导入社区养老系统 SQL。
2. 修改 community-elderly-care 后端配置中的数据库连接。
3. 在后端目录执行 mvn spring-boot:run 启动接口服务。
4. 进入 community-elderly-care-admin 执行 npm install。
5. 执行 npm run dev 启动管理端并配置后端 API 地址。

## 项目结构

- 新/community-elderly-care：后端服务
- 新/community-elderly-care-admin：Vue 管理端
- README/技术文档：项目说明资料（上传时保留根 README，其他论文类材料过滤）

## 上传说明

本仓库只保留项目运行和二次开发所需的代码、配置、数据库脚本及少量必要静态资源。

以下内容不会上传：论文、答辩材料、临时文档、依赖目录、构建产物、压缩包、数据集、模型权重、视频、日志、本地工具包以及密钥配置。
