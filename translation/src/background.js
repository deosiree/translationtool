'use strict'

import { app, protocol, BrowserWindow ,Menu,ipcMain,globalShortcut,dialog} from 'electron'
import { createProtocol } from 'vue-cli-plugin-electron-builder/lib'
import installExtension, { VUEJS3_DEVTOOLS } from 'electron-devtools-installer'
const isDevelopment = process.env.NODE_ENV !== 'production'

// 新增拉起jar包
const { exec,spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');

// Scheme must be registered before the app is ready
protocol.registerSchemesAsPrivileged([
  { scheme: 'app', privileges: { secure: true, standard: true } }
])
let win;
async function createWindow() {
  // Create the browser window.
  win = new BrowserWindow({
    // width: 800,
    // height: 600,
    width: 1600,
    height: 864,
    frame: false,//去除原有的标题栏
    webPreferences: {
      
      // Use pluginOptions.nodeIntegration, leave this alone
      // See nklayman.github.io/vue-cli-plugin-electron-builder/guide/security.html#node-integration for more info
      nodeIntegration: process.env.ELECTRON_NODE_INTEGRATION,
      contextIsolation: !process.env.ELECTRON_NODE_INTEGRATION
    }
    
  })
  
  if (process.env.WEBPACK_DEV_SERVER_URL) {
    // Load the url of the dev server if in development mode
    await win.loadURL(process.env.WEBPACK_DEV_SERVER_URL)
    if (!process.env.IS_TEST) win.webContents.openDevTools()
  } else {
    createProtocol('app')
    // Load the index.html when not in development
    win.loadURL('app://./index.html')
  }
   //开启开发者工具
  globalShortcut.register('Alt+Shift+D', () => {
    win.webContents.openDevTools({mode:'detach'}) //开启开发者工具
  })
}
// 删除默认菜单
Menu.setApplicationMenu(null)
ipcMain.on('min', e=> win.minimize());
ipcMain.on('max', e=> {
    if (win.isMaximized()) {
      win.unmaximize()
    } else {
      win.maximize()
    }
});
ipcMain.on('close', e=> win.close());
// Quit when all windows are closed.
app.on('window-all-closed', () => {
  // On macOS it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  // On macOS it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', async () => {
  if (isDevelopment && !process.env.IS_TEST) {
    // Install Vue Devtools
    try {
      await installExtension(VUEJS3_DEVTOOLS)
    } catch (e) {
      console.error('Vue Devtools failed to install:', e.toString())
    }
  }
  createWindow()

  // 启动java服务
  // startServer()
  
})

// Exit cleanly on request from parent process in development mode.
if (isDevelopment) {
  if (process.platform === 'win32') {
    process.on('message', (data) => {
      if (data === 'graceful-exit') {
        app.quit()
      }
    })
  } else {
    process.on('SIGTERM', () => {
      app.quit()
    })
  }
}

// 启动服务
function startServer(){
  //新增拉起jar包
  checkJavaInstalled()
    .then(() => {
      // 如果 Java 已安装，直接运行 JAR 文件
      runJar();
    })
    .catch(async (err) => {
      console.error(err);

      // 提示用户安装 Java
      const result = await dialog.showMessageBox({
        type: 'warning',
        title: 'Java Not Found',
        message: 'Java is not installed on your system. Do you want to install it?',
        buttons: ['Yes', 'No']
      });

      if (result.response === 0) {
        // 如果用户选择安装 Java，执行安装流程
        try {
          await installJava();
          runJar();  // 安装完成后，启动 JAR 文件
        } catch (installError) {
          dialog.showErrorBox('Java Installation Failed', installError);
        }
      } else {
        dialog.showErrorBox('Java Not Found', 'You need Java to run this application.');
        app.quit();
      }
    });
}

// 检查 Java 是否已安装
function checkJavaInstalled() {
  return new Promise((resolve, reject) => {
    exec('java -version', (error, stdout, stderr) => {
      if (error) {
        reject('Java is not installed or not in PATH');
      } else {
        resolve('Java is installed');
      }
    });
  });
}

// 安装 Java (如果未安装)
function installJava() {
  return new Promise((resolve, reject) => {
    const exeDirectory = process.resourcesPath;
    const javaInstallerDir = path.join(exeDirectory, 'env', 'jdk');
    let installerPath;
    showLoadingWindow()
    // 根据平台选择安装包
    if (os.platform() === 'win32') {
      installerPath = path.join(javaInstallerDir, 'jdk-8u152-windows-x64.exe');
      const installDir = 'C:\\Program Files\\Java\\jdk1.8.0_291';  // 固定安装路径
      // 执行安装命令，并指定安装目录
      exec(`"${installerPath}" /s INSTALLDIR="${installDir}"`, (err, stdout, stderr) => {
        if (err) {
          return reject('Failed to install Java on Windows');
        }

        // 安装完成后设置环境变量
        setWindowsEnvironmentVariables(installDir)
          .then(() => {
            resolve('Java installed and environment variables set');
          })
          .catch(reject);
        if(loadingWindow){
          loadingWindow.close()
        }
        dialog.showMessageBox({
          type: 'info',
          title: '提示',
          message: 'java install success!'
        });
      });
        
    } else {
      if(loadingWindow){
        loadingWindow.close()
      }
      reject('Unsupported platform for automatic Java installation');
    }
  });
}

let jarProcess = null;
// 启动 JAR 包
function runJar() {

  showLoadingWindow()
  const exeDirectory = process.resourcesPath;
  const jarPath = path.join(exeDirectory, 'env','server', 'translationtoolservice-0.0.1-SNAPSHOT.jar');
  jarProcess = spawn('java', ['-jar', jarPath]);

  if(loadingWindow){
    loadingWindow.close()
  }
}

// 配置环境变量
function setWindowsEnvironmentVariables(javaPath) {
  return new Promise((resolve, reject) => {
    // 设置 JAVA_HOME 环境变量
    exec(`setx JAVA_HOME "${javaPath}"`, (err, stdout, stderr) => {
      if (err) {
        return reject('Failed to set JAVA_HOME environment variable');
      }
      // 更新 PATH 环境变量
      exec(`setx PATH "%PATH%;${javaPath}\\bin"`, (err, stdout, stderr) => {
        if (err) {
          return reject('Failed to update PATH environment variable');
        }
        resolve('Environment variables set successfully');
      });
    });
  });
}

// 关闭应用时确保关闭 JAR 进程
app.on('before-quit', () => {
  if (jarProcess) {
    jarProcess.kill();  // 关闭 JAR 进程
    console.log('JAR process killed');
  }
});

// 加载中动画
let loadingWindow
function showLoadingWindow() {
  loadingWindow = new BrowserWindow({
    width: 400,
    height: 300,
    frame: true,  
    alwaysOnTop: true,  // 始终在最前面
    transparent: true,  // 背景透明
  });

  const exeDirectory = process.resourcesPath;
  const loadingPath = path.join(exeDirectory, 'env', 'loading.html');
  loadingWindow.loadURL(loadingPath);  // 加载包含加载动画的HTML页面
  loadingWindow.show();
}