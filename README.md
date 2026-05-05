# TMXValidator

![alt text](https://maxprograms.com/images/Red_squares.png "TMXValidator Icon")

Check the validity of your TMX documents on Windows, Linux or macOS with TMXValidator.

Most CAT (Computer Aided Translation) tools rely on TMX (Translation Memory eXchange) standard to exchange translation memory data. Unfortunately, some tools produce files that are not valid and others do not accept correctly formatted TMX documents.

TMXValidator validates your documents against the TMX DTD and also verifies that they comply with the requirements described in the TMX specifications.

TMXValidator supports TMX versions 1.1, 1.2, 1.3 and 1.4.

The source code of TMXValidator was originally published on SourceForge at [https://sourceforge.net/p/tmxvalidator/code](https://sourceforge.net/p/tmxvalidator/code). The original version was written in Java and loaded the TMX file into memory for validation, limiting support for very large files.

TMXValidator has since been rewritten entirely in TypeScript. It uses a SAX-based streaming parser with full support for DTD validation ([TypesXML](https://maxprograms.com/products/typesxml.html)) and has no file size limitation.

## Releases

Version | Comment | Release Date
------- | ------- | ------------
3.0.0 | Rewritten entirely in TypeScript and removed Java code | April 6, 2026
2.8.0 | Added Spanish and French localization | August 3, 2025
2.7.0 | Added support for huge files | March 22, 2024
2.6.0 | Tighter checking of "x" and "i" attributes and language codes | July 4, 2023
2.5.0 | Updated code and libraries | May 22, 2023
2.4.0 | Updated libraries | December 8, 2022
2.3.0 | Updated code and libraries | February 17, 2022
2.2.0 | Updated libraries and TypeScript code | January 2, 2021
2.1.0 | Added UI written in TypeScript and improved validation | February 5, 2020
2.0.2 | Switched to ant for building and updated OpenXLIFF | August 8, 2019
2.0.1 | Fixed date validation and updated libraries | June 24, 2019
2.0.0 | New version that supports validation of very large files | November 28, 2018

Ready to use installers are available at [https://www.maxprograms.com/products/tmxvalidator.html](https://www.maxprograms.com/products/tmxvalidator.html)

## Requirements

- Node.js 24.14.0 LTS or newer. Get it from [https://nodejs.org/](https://nodejs.org/)

## Building

- Clone this repository.
- Run `npm install` to download and install NodeJS dependencies
- Run `npm start` to launch TMXValidator

``` bash
  git clone https://github.com/maxprograms-com/TMXValidator.git
  cd TMXValidator
  npm install
  npm start
```
