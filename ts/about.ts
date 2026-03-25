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
import {ipcRenderer, IpcRendererEvent} from 'electron';

export class About {


    constructor() {
        ipcRenderer.send('get-version');
        ipcRenderer.on('set-version', (event: IpcRendererEvent, arg: any) => {
            (document.getElementById('version') as HTMLSpanElement).innerHTML = arg.version;
            (document.getElementById('build') as HTMLSpanElement).innerHTML = arg.build;
        });
        setTimeout(() => {
            ipcRenderer.send('set-size', { window: 'about', width: document.body.clientWidth, height: document.body.clientHeight });
        }, 150);
    }
}