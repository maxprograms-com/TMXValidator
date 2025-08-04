/*******************************************************************************
 * Copyright (c) 2005-2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

class Main {

    electron = require('electron');

    constructor() {
        document.getElementById('browse').addEventListener('click', () => {
            this.browse();
        });
        document.getElementById('about').addEventListener('click', () => {
            this.showAbout();
        });
        document.getElementById('validate').addEventListener('click', () => {
            this.validate();
        });
        this.electron.ipcRenderer.on('add-tmx-validation', (event: Electron.IpcRendererEvent, arg: any) => {
            (document.getElementById('tmxFile') as HTMLInputElement).value = arg;
        });

        this.electron.ipcRenderer.on('validation-started', () => {
            document.documentElement.style.cursor = 'wait';
            document.getElementById('working').style.display = 'block';
        });

        this.electron.ipcRenderer.on('validation-completed', () => {
            document.documentElement.style.cursor = 'default';
            document.getElementById('working').style.display = 'none';
        });
        setTimeout(() => {
            this.electron.send('set-size', { window: 'main', width: document.body.clientWidth, height: document.body.clientHeight });
        }, 150);
    }

    browse() {
        this.electron.ipcRenderer.send('select-tmx-validation');
    }

    validate() {
        let tmxfile: string = (document.getElementById('tmxFile') as HTMLInputElement).value;
        if (tmxfile === '') {
            this.electron.ipcRenderer.send('select-file');
            return;
        }
        this.electron.ipcRenderer.send('validate', { command: 'validate', file: tmxfile });
    }

    showAbout() {
        this.electron.ipcRenderer.send('show-about');
    }
}
