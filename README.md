# data-manager
> 原仓库地址：https://github.com/GZTipDM/TipDM
## 1. install front

### 1.1 install dependencies

```shell
npm install
```

### 1.2 config service

When you run the app, you need to configure the service in `D:\work\data\data-manager\frontend\static\config.json`. The configuration is as follows:
```json
{
    "httpServer": "http://localhost:8088/dmserver", // http server address
    "httpOauth": "http://localhost:8082/oauth", // oauth address
    "httpClient": "http://localhost:8089", // client address
    "socketServer": "localhost",
    "socketPort": 9020,
    "title": "数据挖掘建模平台",
    "databaseUploadFileSize": 10,
    "mode": "online"
}
```

### 1.3 run front

```shell
npm run dev
```

