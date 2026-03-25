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

import {ipcRenderer,IpcRendererEvent} from "electron";
export class Main {

    electron = require('electron');

    constructor() {
        (document.getElementById('browse') as HTMLButtonElement).addEventListener('click', () => {
            this.browse();
        });
        (document.getElementById('about') as HTMLButtonElement).addEventListener('click', () => {
            this.showAbout();
        });
        (document.getElementById('validate') as HTMLButtonElement).addEventListener('click', () => {
            this.validate();
        });
        ipcRenderer.on('add-tmx-validation', (event: IpcRendererEvent, arg: any) => {
            (document.getElementById('tmxFile') as HTMLInputElement).value = arg;
        });

        ipcRenderer.on('validation-started', () => {
            document.documentElement.style.cursor = 'wait';
            (document.getElementById('working') as HTMLImageElement).style.display = 'block';
        });

        ipcRenderer.on('validation-completed', () => {
            document.documentElement.style.cursor = 'default';
            (document.getElementById('working') as HTMLImageElement).style.display = 'none';
        });
        setTimeout(() => {
            ipcRenderer.send('set-size', { window: 'main', width: document.body.clientWidth, height: document.body.clientHeight });
        }, 150);
    }

    browse() {
        ipcRenderer.send('select-tmx-validation');
    }

    validate() {
        let tmxfile: string = (document.getElementById('tmxFile') as HTMLInputElement).value;
        if (tmxfile === '') {
            ipcRenderer.send('select-file');
            return;
        }
        ipcRenderer.send('validate', { command: 'validate', file: tmxfile });
    }

    showAbout() {
        ipcRenderer.send('show-about');
    }
}
