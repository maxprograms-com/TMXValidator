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

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.IContentHandler;

public class TMXValidatingHandler implements IContentHandler {

	public static final String RELOAD = Messages.getString("TMXValidatingHandler.0");
	private Element current;
	Stack<Element> stack;
	Map<String, Set<String>> xMap;
	private boolean inCDATA = false;
	private String srcLang;
	private Element root;

	private static final Logger LOGGER = System.getLogger(TMXValidatingHandler.class.getName());

	private Hashtable<String, String> ids;
	private int balance;
	private String version;
	private String publicId;
	private String systemId;
	private String currentLang;

	public TMXValidatingHandler() {
		stack = new Stack<>();
	}

	@Override
	public void startDTD(String name, String publicId1, String systemId1) throws SAXException {
		this.publicId = publicId1;
		this.systemId = systemId1;
	}

	@Override
	public void endDTD() throws SAXException {
		// do nothing
	}

	@Override
	public void startEntity(String name) throws SAXException {
		// do nothing, let the EntityResolver handle this
	}

	@Override
	public void endEntity(String name) throws SAXException {
		// do nothing, let the EntityResolver handle this
	}

	@Override
	public void startCDATA() throws SAXException {
		inCDATA = true;
	}

	@Override
	public void endCDATA() throws SAXException {
		inCDATA = false;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		// do nothing
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// do nothing
	}

	@Override
	public void startDocument() throws SAXException {
		// do nothing
	}

	@Override
	public void endDocument() throws SAXException {
		stack.clear();
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// do nothing
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// do nothing
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (current == null) {
			current = new Element(qName);
			stack.push(current);
		} else {
			Element child = new Element(qName);
			current.addContent(child);
			stack.push(current);
			current = child;
		}
		for (int i = 0; i < atts.getLength(); i++) {
			current.setAttribute(atts.getQName(i), atts.getValue(i));
		}
		if (root == null) {
			if (qName.equals("tmx")) {
				root = current;
				version = root.getAttributeValue("version");
				if (version.isEmpty()) {
					throw new SAXException(Messages.getString("TMXValidatingHandler.1"));
				}
				if (!(version.equals("1.1") || version.equals("1.2") || version.equals("1.3")
						|| version.equals("1.4"))) {
					MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.2"));
					throw new SAXException(mf.format(new Object[] { version }));
				}
				if (systemId == null && publicId == null) {
					throw new SAXException(RELOAD);
				}
			} else {
				throw new SAXException(Messages.getString("TMXValidatingHandler.3"));
			}
		}
		if (qName.equals("header")) {
			srcLang = current.getAttributeValue("srclang");
			if (srcLang.isEmpty()) {
				throw new SAXException(Messages.getString("TMXValidatingHandler.4"));
			}
			if (!srcLang.equals("*all*")) {
				try {
					if (!checkLang(srcLang)) {
						MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.5"));
						throw new SAXException(mf.format(new Object[] { srcLang }));
					}
				} catch (IOException | ParserConfigurationException e) {
					LOGGER.log(Level.ERROR, Messages.getString("TMXValidatingHandler.6"), e);
					throw new SAXException(Messages.getString("TMXValidatingHandler.6"));
				}
			}
		}
		if ("tu".equals(qName)) {
			xMap = new Hashtable<>();
		}
		if ("tuv".equals(qName)) {
			currentLang = current.hasAttribute("xml:lang") ? current.getAttributeValue("xml:lang")
					: current.getAttributeValue("lang");
		}
		if (current.hasAttribute("x")
				&& ("bpt".equals(qName) || "it".equals(qName) || "ph".equals(qName) || "hi".equals(qName))) {
			String x = current.getAttributeValue("x");
			if (!isNumber(x)) {
				MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.7"));
				throw new SAXException(mf.format(new Object[] { x }));
			}
			Set<String> set = xMap.get(currentLang);
			if (set == null) {
				set = new HashSet<>();
				xMap.put(currentLang, set);
			}
			if (set.contains(qName + x)) {
				MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.8"));
				throw new SAXException(mf.format(new Object[] { x, qName }));
			}
			set.add(qName + x);
		}
	}

	private boolean isNumber(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (current == null) {
			return;
		}
		List<Attribute> attributes = current.getAttributes();
		Iterator<Attribute> i = attributes.iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			String name = a.getName();
			String value = a.getValue();
			if (name.equals("lang") || name.equals("adminlang") || name.equals("xml:lang")) {
				try {
					if (!checkLang(value)) {
						MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.9"));
						throw new SAXException(mf.format(new Object[] { value }));
					}
				} catch (IOException | ParserConfigurationException e) {
					LOGGER.log(Level.ERROR, Messages.getString("TMXValidatingHandler.10"), e);
					throw new SAXException(Messages.getString("TMXValidatingHandler.10"));
				}
			}
			if (name.equals("usagecount") && !isNumber(value)) {
				MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.11"));
				throw new SAXException(mf.format(new Object[] { value }));
			}
			if ((name.equals("lastusagedate") || name.equals("changedate") || name.equals("creationdate"))
					&& !checkDate(value)) {
				MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.12"));
				throw new SAXException(mf.format(new Object[] { value }));
			}
		}
		if (current.getName().equals("seg")) {
			balance = 0;
			ids = null;
			ids = new Hashtable<>();
			recurse(current);
			if (balance != 0) {
				MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.13"));
				throw new SAXException(mf.format(new String[] { current.toString() }));
			}
			if (ids.size() > 0) {
				Enumeration<String> en = ids.keys();
				while (en.hasMoreElements()) {
					if (!ids.get(en.nextElement()).equals("0")) {
						MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.14"));
						throw new SAXException(mf.format(new String[] { current.toString() }));
					}
				}
			}
		}
		if (localName.equals("tu")) {
			if (!srcLang.equals("*all*")) {
				checkLanguageVariants(current);
			}
			Set<String> xKeys = xMap.keySet();
			if (!xKeys.isEmpty()) {
				if (current.getChildren("tuv").size() != xKeys.size()) {
					throw new SAXException(Messages.getString("TMXValidatingHandler.15"));
				}
				Set<String> xValues = xMap.get(currentLang);
				Iterator<String> it = xKeys.iterator();
				while (it.hasNext()) {
					String key = it.next();
					Set<String> langSet = xMap.get(key);
					if (langSet.size() != xValues.size() || !langSet.containsAll(xValues)) {
						throw new SAXException(Messages.getString("TMXValidatingHandler.16"));
					}
				}
			}
			current = null;
			stack.clear();
		}
		if (!stack.isEmpty()) {
			current = stack.pop();
		}
	}

	private void checkLanguageVariants(Element tu) throws SAXException {
		List<Element> variants = tu.getChildren("tuv");
		Iterator<Element> it = variants.iterator();
		boolean found = false;
		while (it.hasNext() && !found) {
			Element tuv = it.next();
			String lang = tuv.getAttributeValue("xml:lang");
			if (lang.isEmpty() && (version.equals("1.1") || version.equals("1.2"))) {
				lang = tuv.getAttributeValue("lang");
			}
			if (lang.isEmpty()) {
				throw new SAXException(Messages.getString("TMXValidatingHandler.17"));
			}
			if (lang.equalsIgnoreCase(srcLang)) {
				found = true;
			}
		}
		if (!found) {
			MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.18"));
			throw new SAXException(mf.format(new Object[] { srcLang }));
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (!inCDATA && current != null) {
			current.addContent(new String(ch, start, length));
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		// do nothing
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		// do nothing
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// do nothing
	}

	private boolean checkLang(String lang) throws IOException, SAXException, ParserConfigurationException {
		if (lang.startsWith("x-") || lang.startsWith("X-")) {
			// custom language code
			return true;
		}
		Language language = LanguageUtils.getLanguage(lang);
		if (language == null) {
			return false;
		}
		return language.getCode().equals(lang);
	}

	private static boolean checkDate(String date) {
		// YYYYMMDDThhmmssZ
		if (date.length() != 16) {
			return false;
		}
		if (date.charAt(8) != 'T') {
			return false;
		}
		if (date.charAt(15) != 'Z') {
			return false;
		}
		try {
			int year = Integer.parseInt("" + date.charAt(0) + date.charAt(1) + date.charAt(2) + date.charAt(3));
			if (year < 0) {
				return false;
			}
			int month = Integer.parseInt("" + date.charAt(4) + date.charAt(5));
			if (month < 1 || month > 12) {
				return false;
			}
			int day = Integer.parseInt("" + date.charAt(6) + date.charAt(7));
			switch (month) {
				case 1, 3, 5, 7, 8, 10, 12 -> {
					if (day < 1 || day > 31) {
						return false;
					}
				}
				case 4, 6, 9, 11 -> {
					if (day < 1 || day > 30) {
						return false;
					}
				}
				case 2 -> {
					// check for leap years
					if (year % 4 == 0) {
						if (year % 100 == 0) {
							// not all centuries are leap years
							if (year % 400 == 0) {
								if (day < 1 || day > 29) {
									return false;
								}
							} else {
								// not leap year
								if (day < 1 || day > 28) {
									return false;
								}
							}
						}
						if (day < 1 || day > 29) {
							return false;
						}
					} else if (day < 1 || day > 28) {
						return false;
					}
				}
				default -> {
					return false;
				}
			}
			int hour = Integer.parseInt("" + date.charAt(9) + date.charAt(10));
			if (hour < 0 || hour > 23) {
				return false;
			}
			int min = Integer.parseInt("" + date.charAt(11) + date.charAt(12));
			if (min < 0 || min > 59) {
				return false;
			}
			int sec = Integer.parseInt("" + date.charAt(13) + date.charAt(14));
			if (sec < 0 || sec > 59) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private void recurse(Element element) throws SAXException {
		List<Element> children = element.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			if (e.getName().equals("bpt")) {
				balance += 1;
				if (version.equals("1.4")) {
					String s = e.getAttributeValue("i");
					if (!isNumber(s)) {
						MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.19"));
						throw new SAXException( mf.format(new String[] { element.toString() }));
					}
					if (!ids.containsKey(s)) {
						ids.put(s, "1");
					} else {
						if (ids.get(s).equals("-1")) {
							ids.put(s, "0");
						} else {
							MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.20"));
							throw new SAXException(mf.format(new String[] { element.toString() }));
						}
					}
				}
			}
			if (e.getName().equals("ept")) {
				balance -= 1;
				if (version.equals("1.4")) {
					String s = e.getAttributeValue("i");
					if (!isNumber(s)) {
						MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.21"));
						throw new SAXException(mf.format(new String[] { element.toString() }));
					}
					if (!ids.containsKey(s)) {
						ids.put(s, "-1");
					} else {
						if (ids.get(s).equals("1")) {
							ids.put(s, "0");
						} else {
							MessageFormat mf = new MessageFormat(Messages.getString("TMXValidatingHandler.22"));
							throw new SAXException(mf.format(new String[] { element.toString() }));
						}
					}
				}
			}
			recurse(e);
		}
	}

	@Override
	public Document getDocument() {
		return null;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public void setCatalog(Catalog arg0) {
		// do nothing
	}
}
