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

class About {

    electron = require('electron');

    constructor() {
        this.electron.ipcRenderer.send('get-version');
        this.electron.ipcRenderer.on('set-version', (event: Electron.IpcRendererEvent, arg: any) => {
            document.getElementById('version').innerHTML = arg.version;
            document.getElementById('build').innerHTML = arg.build;
        });
        setTimeout(() => {
            this.electron.send('set-size', { window: 'about', width: document.body.clientWidth, height: document.body.clientHeight });
        }, 150);
    }
}