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
package com.maxprograms.tmxvalidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class ValidationServer implements HttpHandler {

	private static final Logger LOGGER = System.getLogger(ValidationServer.class.getName());

	private HttpServer server;
	private Map<String, String> running;
	private Map<String, JSONObject> validationResults;

	public ValidationServer(int port) throws IOException {
		running = new Hashtable<>();
		validationResults = new Hashtable<>();
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/ValidationServer", this);
		server.setExecutor(null); // creates a default executor
	}

	public static void main(String[] args) {
		String port = "8010";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-version")) {
				MessageFormat mf = new MessageFormat(Messages.getString("ValidationServer.0") );
				LOGGER.log(Level.INFO, () -> mf.format(new String[] {Constants.VERSION, Constants.BUILD}));
				return;
			}
			if (arg.equals("-port") && (i + 1) < args.length) {
				port = args[i + 1];
			}
		}
		try {
			ValidationServer instance = new ValidationServer(Integer.valueOf(port));
			instance.run();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e);
		}
	}

	private void run() {
		server.start();
		LOGGER.log(Level.INFO, Messages.getString("ValidationServer.1"));
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		JSONObject json = null;

		String response = "";
		String command = "version";
		try {
			try (InputStream is = t.getRequestBody()) {
				json = readRequestBody(is);
			}
			if (json.has("command")) {
				command = json.getString("command");
				if ("version".equals(command)) {
					JSONObject result = new JSONObject();
					result.put("version", Constants.VERSION);
					result.put("build", Constants.BUILD);
					response = result.toString();
				} else if ("validate".equals(command)) {
					response = validate(json);
				} else if ("status".equals(command)) {
					response = getStatus(json);
				} else if ("validationResult".equals(command)) {
					response = getValidationResult(json);
				} else {
					response = "{\"reason\":\"" + Messages.getString("ValidationServer.2") + "\"}";
				}
			} else {
				response = "{\"reason\":\"" + Messages.getString("ValidationServer.3") + "\"}";
			}
			t.getResponseHeaders().add("content-type", "application/json; charset=utf-8");
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			t.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = t.getResponseBody()) {
				os.write(bytes);
			}
		} catch (IOException | JSONException e) {
			response = e.getMessage();
			t.sendResponseHeaders(500, response.length());
			try (OutputStream os = t.getResponseBody()) {
				os.write(response.getBytes());
			}
		}
	}

	private static JSONObject readRequestBody(InputStream is) throws IOException, JSONException {
		StringBuilder request = new StringBuilder();
		try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = rd.readLine()) != null) {
				if (!request.isEmpty()) {
					request.append('\n');
				}
				request.append(line);
			}
		}
		return new JSONObject(request.toString());
	}

	private String getStatus(JSONObject json) {
		JSONObject result = new JSONObject();
		if (!json.has("process")) {
			result.put("status", Constants.ERROR);
			result.put("reason", Messages.getString("ValidationServer.4"));
			return result.toString();
		}
		String process = json.getString("process");
		String status = running.get(process);
		if (status != null) {
			result.put("status", status);
		} else {
			result.put("status", Constants.ERROR);
			result.put("reason", Messages.getString("ValidationServer.5"));
		}
		return result.toString();
	}

	private String getValidationResult(JSONObject json) {
		JSONObject result = new JSONObject();
		if (!json.has("process")) {
			result.put("status", Constants.ERROR);
			result.put("reason", Messages.getString("ValidationServer.4"));
			return result.toString();
		}
		String process = json.getString("process");
		if (validationResults.containsKey(process)) {
			result = validationResults.get(process);
			validationResults.remove(process);
			return result.toString();
		}
		result.put("status", Constants.ERROR);
		result.put("reason", Messages.getString("ValidationServer.6"));
		return result.toString();
	}

	private String validate(JSONObject json) {
		JSONObject result = new JSONObject();
		if (!json.has("file")) {
			result.put("status", Constants.ERROR);
			result.put("reason", Messages.getString("ValidationServer.7"));
			return result.toString();
		}
		String file = json.getString("file");
		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process, Constants.RUNNING);
				JSONObject result = new JSONObject();
				TMXValidator validator = new TMXValidator();
				try {
					validator.validate(new File(file));
					result.put("valid", true);
					result.put("comment", Messages.getString("ValidationServer.8"));
				} catch (IOException | SAXException | ParserConfigurationException e) {
					result.put("valid", false);
					String reason = e.getMessage();
					if (reason.indexOf('\n') != -1) {
						reason = reason.substring(0, reason.indexOf('\n'));
					}
					result.put("reason", reason);
				}
				validationResults.put(process, result);
				if (running.get(process).equals(Constants.RUNNING)) {
					running.put(process, Constants.COMPLETED);
				}
			}
		}).start();
		result.put("status", Constants.SUCCESS);
		result.put("process", process);
		return result.toString();
	}
}
