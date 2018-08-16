/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
#!/usr/bin/env node

'use strict';

// A script to assist with CoffeeScript -> JS
// conversion.
//
// This script uses 'decaffeinate' to perform
// an initial CoffeeScript -> ES2015 conversion,
// then reformats the result using TypeScript formatter
// and JSCS
//
// The conversion process for H source is as follows:
//
//  1. Run this script on the .coffee file(s) to
//     perform an initial conversion.
//
//     If there is anything in the CoffeeScript that
//     decaffeinate cannot handle, you might need to
//     simplify the offending CoffeeScript (as indicated
//     by the error output) and try again.
//
//  2. Remove the input .coffee files from the source tree.
//  3. Remove any ES2015-isms which are not currently allowed
//     in the main codebase, also check for common issues
//     in the converted source from the list below.
//  4. Re-run the tests and verify that everything works.
//  5. Repeat steps 1-4 with the '-test.coffee' file that
//     corresponds to the converted source file.
//
// Issues to look out for in the converted source:
//
//  - Run JSHint on the generated output and check for any
//    violations (eg. unused variables)
//  - Unnecessary 'return' statements in the last
//    line of a function. CoffeeScript implicitly returns
//    the last expression in a function, so the generated
//    JS source has to do the same.

var Checker = require('jscs');
var babylon = require('babylon');
var decaffeinate = require('decaffeinate');
var fs = require('fs');
var path = require('path');
var typescriptFormatter = require('typescript-formatter');

var inFile = process.argv[2];

var jscsConfigPath = require.resolve('../../.jscsrc');
var jscsConfig = JSON.parse(fs.readFileSync(jscsConfigPath, 'utf-8'));

var stripReturnPatterns = [
  // Unit test cases
  /it\(/,
  // Assignments in setters etc.
  /[^=]+=[^=]+/,
];

/**
 * Strip unnecessary 'return' statements from the last line of
 * functions.
 *
 * In CoffeeScript, the last expression in a function is implicitly
 * returned. Since the intended return type is unknown, 'decaffeinate'
 * has to assume that the result _might_ be used.
 *
 * This function converts 'return <statement>' to '<statement>' for
 * common cases in our codebase where we know that '<statement>'
 * is not meant to be used as the return value.
 */
function stripUnnecessaryReturns(js) {
  // This implementation is very stupid but works because we are
  // only dealing with very simple expressions.
  //
  // If we need something more sophisticated, we shouldn't modify the
  // source as a string but should instead write a Babel code transformer.
  return js.split('\n').map(line => {
    var returnPrefix = 'return ';
    if (line.trim().startsWith(returnPrefix)) {
      var remainder = line.trim().slice(returnPrefix.length);
      for (var i=0; i < stripReturnPatterns.length; i++) {
        if (remainder.match(stripReturnPatterns[i])) {
          return remainder;
        }
      }
      return line;
    } else {
      return line;
    }
  }).join('\n');
}

/**
 * Attempt to parse the input as ES2015 JS.
 *
 * @param {string} js - The input ES2015 JavaScript to parse.
 * @throws {Error} An error with context information if the input JS
 *                 cannot be parsed.
 */
function checkSyntax(js) {
  try {
    babylon.parse(js, {sourceType: 'module'});
  } catch (err) {
    var context = js.split('\n').reduce((context, line, index) => {
      var lineNumber = index+1;
      var linePrefix;
      if (lineNumber === err.loc.line) {
        linePrefix = `**${lineNumber}`;
      } else {
        linePrefix = `  ${lineNumber}`;
      }
      if (Math.abs(lineNumber - err.loc.line) < 10) {
        return context.concat(`${linePrefix}: ${line}`);
      } else {
        return context;
      }
    }, []).join('\n');
    throw new Error(
        `Could not parsing ES2015 JavaScript generated by 'decaffeinate'.
        You may need to fix or simplify the CoffeeScript first.

        Error: ${err}
        Context:\n${context}\n\n`);
  }
}

function reformat(js) {
  // 1. Use Babylon (Babel's JS parser) to parse the generated JS
  //    and verify that it is syntactically valid.
  // 2. Use typescript-formatter to do an initial pass over
  //    the code and correct indentation etc.
  // 3. Finally, use JSCS to clean up smaller issues

  js = `'use strict';\n\n${js}`;
  js = stripUnnecessaryReturns(js);

  try {
    checkSyntax(js);
  } catch (err) {
    return Promise.reject(err);
  }

  return typescriptFormatter.processString(inFile, js, {
    baseDir: __dirname,
    tsfmt: true,
  }).then(result => {
    return result.dest;
  })
    .then(result => {
      var checker = new Checker();
      checker.configure(jscsConfig);
      return checker.fixString(result).output;
    });
}

function toResultOrError(promise) {
  return promise.then(result => {
    return { result: result };
  }).catch(err => {
    return { error: err };
  });
}

function convertFile(inFile, outFile) {
  console.log('Converting', inFile);

  var js;

  try {
    js = decaffeinate.convert(
      fs.readFileSync(inFile).toString('utf8')
    );
  } catch (err) {
    return Promise.reject(err);
  }

  return reformat(js).then(result => {
    fs.writeFileSync(outFile, result);
  });
}

var conversions = [];
process.argv.slice(2).forEach(filePath => {
  var inFile = path.resolve(filePath);
  var outFile = inFile.replace(/\.coffee$/, '.js');
  conversions.push(toResultOrError(convertFile(inFile, outFile)).then(function (result) {
    result.fileName = inFile;
    return result;
  }));
});

Promise.all(conversions).then(results => {
  var ok = 0;
  var failed = 0;
  results.forEach(result => {
    if (result.error) {
      console.log('Error converting %s: \n\n%s', result.fileName, result.error.message);
      ++failed;
    } else {
      console.log('Converted %s', result.fileName);
      ++ok;
    }
  });
  console.log('Converted %d files, failed to convert %d files', ok, failed);
}).catch(err => {
  console.log('Conversion error:', err);
});
