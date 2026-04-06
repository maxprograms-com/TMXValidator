/*******************************************************************************
 * Copyright (c) 2005-2026 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

import { app, ipcMain, BrowserWindow, dialog } from "electron";
import { IpcMainEvent } from "electron/main";
import { Worker } from "node:worker_threads";
import { I18n } from "./i18n.js";

class TMXValidator {

    static mainWindow: BrowserWindow;
    static appLang: string = 'en';
    static i18n: I18n;
    static path = require('path');

    constructor() {
        if (!app.requestSingleInstanceLock()) {
            app.quit()
        } else {
            if (TMXValidator.mainWindow) {
                // Someone tried to run a second instance, we should focus our window.
                if (TMXValidator.mainWindow.isMinimized()) {
                    TMXValidator.mainWindow.restore();
                }
                TMXValidator.mainWindow.focus();
            }
        }
        const defaultLocale: string = Intl.DateTimeFormat().resolvedOptions().locale;
        if (defaultLocale.startsWith('fr')) {
            TMXValidator.appLang = 'fr';
        } else if (defaultLocale.startsWith('es')) {
            TMXValidator.appLang = 'es';
        }
        TMXValidator.i18n = new I18n(TMXValidator.path.join(app.getAppPath(), 'i18n', 'tmxvalidator_' + TMXValidator.appLang + '.json'));
        app.on('ready', () => {
            TMXValidator.createWindows();
            TMXValidator.mainWindow.show();
        });
        app.on('window-all-closed', function () {
            app.quit()
        });
        ipcMain.on('select-file', () => {
            dialog.showErrorBox(TMXValidator.i18n.getString('app', 'attention'), TMXValidator.i18n.getString('app', 'selectFile'));
        });
        ipcMain.on('select-tmx-validation', () => {
            this.selectFile();
        });
        ipcMain.on('show-about', () => {
            this.showAbout();
        });
        ipcMain.on('set-size', (event: IpcMainEvent, arg: {window: string, width: number, height: number }) => {
            if (arg.window === 'about') {
                TMXValidator.mainWindow.setContentSize(arg.width, arg.height, true);
            }
            else if (arg.window === 'main') {
                TMXValidator.mainWindow.setContentSize(arg.width, arg.height, true);
            }
        });
        ipcMain.on('validate', (event: IpcMainEvent, arg: any) => {
            event.sender.send('validation-started');
            let workerPath: string = TMXValidator.path.join(app.getAppPath(), 'js', 'validationWorker.js');
            let worker: Worker = new Worker(workerPath, {
                workerData: {
                    file: arg.file,
                    catalogFile: TMXValidator.path.join(app.getAppPath(), 'catalog', 'catalog.xml'),
                    i18nFile: TMXValidator.path.join(app.getAppPath(), 'i18n', 'tmxvalidator_' + TMXValidator.appLang + '.json')
                }
            });
            worker.on('message', (result: any) => {
                event.sender.send('validation-completed');
                if (result.success) {
                    dialog.showMessageBox({ type: 'info', message: TMXValidator.i18n.getString('tmxvalidator', 'validfile') });
                } else {
                    dialog.showErrorBox(TMXValidator.i18n.getString('app', 'error'), result.message);
                }
            });
            worker.on('error', (error: Error) => {
                event.sender.send('validation-completed');
                dialog.showErrorBox(TMXValidator.i18n.getString('app', 'error'), error.message);
            });
        });
        ipcMain.on('get-version', (event: IpcMainEvent) => {
            event.sender.send('set-version', app.getVersion());
        });
    }

    selectFile(): void {
        dialog.showOpenDialog({
            properties: ['openFile'],
            filters: [
                { name: TMXValidator.i18n.getString('app', 'tmxFiles'), extensions: ['tmx'] },
                { name: TMXValidator.i18n.getString('app', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                TMXValidator.mainWindow.webContents.send('add-tmx-validation', value.filePaths[0]);
            }
        }).catch((reason) => {
            dialog.showErrorBox(TMXValidator.i18n.getString('app', 'error'), reason);
        });
    }

    showAbout(): void {
        let about = new BrowserWindow({
            parent: TMXValidator.mainWindow,
            width: 280,
            height: 290,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: 'img/tmxvalidator.png',
            backgroundColor: '#2d2d2e',
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        let filePath = TMXValidator.path.join(app.getAppPath(), 'html', TMXValidator.appLang, 'about.html');
        let fileUrl: URL = new URL('file://' + filePath);
        about.loadURL(fileUrl.href);
        about.setMenu(null);
        about.show();
    }

    static createWindows() {
        TMXValidator.mainWindow = new BrowserWindow({
            width: 560,
            height: 180,
            show: false,
            maximizable: false,
            icon: 'img/tmxvalidator.png',
            backgroundColor: '#2d2d2e',
            darkTheme: true,
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        TMXValidator.mainWindow.setMenu(null);

        let filePath = TMXValidator.path.join(app.getAppPath(), 'html', TMXValidator.appLang, 'main.html');
        let fileUrl: URL = new URL('file://' + filePath);
        TMXValidator.mainWindow.loadURL(fileUrl.href);
    }

}
try {
    new TMXValidator();
} catch (e) {
    console.error(e);
}