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

import { parentPort, workerData } from "node:worker_threads";
import { I18n } from "./i18n.js";
import { Validator } from "./validator.js";

const i18n: I18n = new I18n(workerData.i18nFile);
const validator: Validator = new Validator(workerData.catalogFile, i18n);
try {
    validator.validateFile(workerData.file);
    parentPort!.postMessage({ success: true });
} catch (error) {
    parentPort!.postMessage({ success: false, message: (error as Error).message });
}
