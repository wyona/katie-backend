(window["webpackJsonp"] = window["webpackJsonp"] || []).push([["styles"],{

/***/ 3:
/*!******************************!*\
  !*** multi ./src/styles.css ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(/*! /Users/michaelwechner/src/wyona/wyona/ask-katie/clients/admin-backend/src/styles.css */"OmL/");


/***/ }),

/***/ "JPst":
/*!*****************************************************!*\
  !*** ./node_modules/css-loader/dist/runtime/api.js ***!
  \*****************************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


/*
  MIT License http://www.opensource.org/licenses/mit-license.php
  Author Tobias Koppers @sokra
*/
// css base code, injected by the css-loader
// eslint-disable-next-line func-names
module.exports = function (useSourceMap) {
  var list = []; // return the list of modules as css string

  list.toString = function toString() {
    return this.map(function (item) {
      var content = cssWithMappingToString(item, useSourceMap);

      if (item[2]) {
        return "@media ".concat(item[2], " {").concat(content, "}");
      }

      return content;
    }).join('');
  }; // import a list of modules into the list
  // eslint-disable-next-line func-names


  list.i = function (modules, mediaQuery, dedupe) {
    if (typeof modules === 'string') {
      // eslint-disable-next-line no-param-reassign
      modules = [[null, modules, '']];
    }

    var alreadyImportedModules = {};

    if (dedupe) {
      for (var i = 0; i < this.length; i++) {
        // eslint-disable-next-line prefer-destructuring
        var id = this[i][0];

        if (id != null) {
          alreadyImportedModules[id] = true;
        }
      }
    }

    for (var _i = 0; _i < modules.length; _i++) {
      var item = [].concat(modules[_i]);

      if (dedupe && alreadyImportedModules[item[0]]) {
        // eslint-disable-next-line no-continue
        continue;
      }

      if (mediaQuery) {
        if (!item[2]) {
          item[2] = mediaQuery;
        } else {
          item[2] = "".concat(mediaQuery, " and ").concat(item[2]);
        }
      }

      list.push(item);
    }
  };

  return list;
};

function cssWithMappingToString(item, useSourceMap) {
  var content = item[1] || ''; // eslint-disable-next-line prefer-destructuring

  var cssMapping = item[3];

  if (!cssMapping) {
    return content;
  }

  if (useSourceMap && typeof btoa === 'function') {
    var sourceMapping = toComment(cssMapping);
    var sourceURLs = cssMapping.sources.map(function (source) {
      return "/*# sourceURL=".concat(cssMapping.sourceRoot || '').concat(source, " */");
    });
    return [content].concat(sourceURLs).concat([sourceMapping]).join('\n');
  }

  return [content].join('\n');
} // Adapted from convert-source-map (MIT)


function toComment(sourceMap) {
  // eslint-disable-next-line no-undef
  var base64 = btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap))));
  var data = "sourceMappingURL=data:application/json;charset=utf-8;base64,".concat(base64);
  return "/*# ".concat(data, " */");
}

/***/ }),

/***/ "LboF":
/*!****************************************************************************!*\
  !*** ./node_modules/style-loader/dist/runtime/injectStylesIntoStyleTag.js ***!
  \****************************************************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


var isOldIE = function isOldIE() {
  var memo;
  return function memorize() {
    if (typeof memo === 'undefined') {
      // Test for IE <= 9 as proposed by Browserhacks
      // @see http://browserhacks.com/#hack-e71d8692f65334173fee715c222cb805
      // Tests for existence of standard globals is to allow style-loader
      // to operate correctly into non-standard environments
      // @see https://github.com/webpack-contrib/style-loader/issues/177
      memo = Boolean(window && document && document.all && !window.atob);
    }

    return memo;
  };
}();

var getTarget = function getTarget() {
  var memo = {};
  return function memorize(target) {
    if (typeof memo[target] === 'undefined') {
      var styleTarget = document.querySelector(target); // Special case to return head of iframe instead of iframe itself

      if (window.HTMLIFrameElement && styleTarget instanceof window.HTMLIFrameElement) {
        try {
          // This will throw an exception if access to iframe is blocked
          // due to cross-origin restrictions
          styleTarget = styleTarget.contentDocument.head;
        } catch (e) {
          // istanbul ignore next
          styleTarget = null;
        }
      }

      memo[target] = styleTarget;
    }

    return memo[target];
  };
}();

var stylesInDom = [];

function getIndexByIdentifier(identifier) {
  var result = -1;

  for (var i = 0; i < stylesInDom.length; i++) {
    if (stylesInDom[i].identifier === identifier) {
      result = i;
      break;
    }
  }

  return result;
}

function modulesToDom(list, options) {
  var idCountMap = {};
  var identifiers = [];

  for (var i = 0; i < list.length; i++) {
    var item = list[i];
    var id = options.base ? item[0] + options.base : item[0];
    var count = idCountMap[id] || 0;
    var identifier = "".concat(id, " ").concat(count);
    idCountMap[id] = count + 1;
    var index = getIndexByIdentifier(identifier);
    var obj = {
      css: item[1],
      media: item[2],
      sourceMap: item[3]
    };

    if (index !== -1) {
      stylesInDom[index].references++;
      stylesInDom[index].updater(obj);
    } else {
      stylesInDom.push({
        identifier: identifier,
        updater: addStyle(obj, options),
        references: 1
      });
    }

    identifiers.push(identifier);
  }

  return identifiers;
}

function insertStyleElement(options) {
  var style = document.createElement('style');
  var attributes = options.attributes || {};

  if (typeof attributes.nonce === 'undefined') {
    var nonce =  true ? __webpack_require__.nc : undefined;

    if (nonce) {
      attributes.nonce = nonce;
    }
  }

  Object.keys(attributes).forEach(function (key) {
    style.setAttribute(key, attributes[key]);
  });

  if (typeof options.insert === 'function') {
    options.insert(style);
  } else {
    var target = getTarget(options.insert || 'head');

    if (!target) {
      throw new Error("Couldn't find a style target. This probably means that the value for the 'insert' parameter is invalid.");
    }

    target.appendChild(style);
  }

  return style;
}

function removeStyleElement(style) {
  // istanbul ignore if
  if (style.parentNode === null) {
    return false;
  }

  style.parentNode.removeChild(style);
}
/* istanbul ignore next  */


var replaceText = function replaceText() {
  var textStore = [];
  return function replace(index, replacement) {
    textStore[index] = replacement;
    return textStore.filter(Boolean).join('\n');
  };
}();

function applyToSingletonTag(style, index, remove, obj) {
  var css = remove ? '' : obj.media ? "@media ".concat(obj.media, " {").concat(obj.css, "}") : obj.css; // For old IE

  /* istanbul ignore if  */

  if (style.styleSheet) {
    style.styleSheet.cssText = replaceText(index, css);
  } else {
    var cssNode = document.createTextNode(css);
    var childNodes = style.childNodes;

    if (childNodes[index]) {
      style.removeChild(childNodes[index]);
    }

    if (childNodes.length) {
      style.insertBefore(cssNode, childNodes[index]);
    } else {
      style.appendChild(cssNode);
    }
  }
}

function applyToTag(style, options, obj) {
  var css = obj.css;
  var media = obj.media;
  var sourceMap = obj.sourceMap;

  if (media) {
    style.setAttribute('media', media);
  } else {
    style.removeAttribute('media');
  }

  if (sourceMap && btoa) {
    css += "\n/*# sourceMappingURL=data:application/json;base64,".concat(btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap)))), " */");
  } // For old IE

  /* istanbul ignore if  */


  if (style.styleSheet) {
    style.styleSheet.cssText = css;
  } else {
    while (style.firstChild) {
      style.removeChild(style.firstChild);
    }

    style.appendChild(document.createTextNode(css));
  }
}

var singleton = null;
var singletonCounter = 0;

function addStyle(obj, options) {
  var style;
  var update;
  var remove;

  if (options.singleton) {
    var styleIndex = singletonCounter++;
    style = singleton || (singleton = insertStyleElement(options));
    update = applyToSingletonTag.bind(null, style, styleIndex, false);
    remove = applyToSingletonTag.bind(null, style, styleIndex, true);
  } else {
    style = insertStyleElement(options);
    update = applyToTag.bind(null, style, options);

    remove = function remove() {
      removeStyleElement(style);
    };
  }

  update(obj);
  return function updateStyle(newObj) {
    if (newObj) {
      if (newObj.css === obj.css && newObj.media === obj.media && newObj.sourceMap === obj.sourceMap) {
        return;
      }

      update(obj = newObj);
    } else {
      remove();
    }
  };
}

module.exports = function (list, options) {
  options = options || {}; // Force single-tag solution on IE6-9, which has a hard limit on the # of <style>
  // tags it will allow on a page

  if (!options.singleton && typeof options.singleton !== 'boolean') {
    options.singleton = isOldIE();
  }

  list = list || [];
  var lastIdentifiers = modulesToDom(list, options);
  return function update(newList) {
    newList = newList || [];

    if (Object.prototype.toString.call(newList) !== '[object Array]') {
      return;
    }

    for (var i = 0; i < lastIdentifiers.length; i++) {
      var identifier = lastIdentifiers[i];
      var index = getIndexByIdentifier(identifier);
      stylesInDom[index].references--;
    }

    var newLastIdentifiers = modulesToDom(newList, options);

    for (var _i = 0; _i < lastIdentifiers.length; _i++) {
      var _identifier = lastIdentifiers[_i];

      var _index = getIndexByIdentifier(_identifier);

      if (stylesInDom[_index].references === 0) {
        stylesInDom[_index].updater();

        stylesInDom.splice(_index, 1);
      }
    }

    lastIdentifiers = newLastIdentifiers;
  };
};

/***/ }),

/***/ "OmL/":
/*!************************!*\
  !*** ./src/styles.css ***!
  \************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

var api = __webpack_require__(/*! ../node_modules/style-loader/dist/runtime/injectStylesIntoStyleTag.js */ "LboF");
            var content = __webpack_require__(/*! !../node_modules/css-loader/dist/cjs.js??ref--12-1!../node_modules/postcss-loader/src??embedded!./styles.css */ "W9N5");

            content = content.__esModule ? content.default : content;

            if (typeof content === 'string') {
              content = [[module.i, content, '']];
            }

var options = {};

options.insert = "head";
options.singleton = false;

var update = api(content, options);



module.exports = content.locals || {};

/***/ }),

/***/ "W9N5":
/*!*********************************************************************************************************************!*\
  !*** ./node_modules/css-loader/dist/cjs.js??ref--12-1!./node_modules/postcss-loader/src??embedded!./src/styles.css ***!
  \*********************************************************************************************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ../node_modules/css-loader/dist/runtime/api.js */ "JPst");
/* harmony import */ var _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_0__);
// Imports

var ___CSS_LOADER_EXPORT___ = _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_0___default()(true);
// Module
___CSS_LOADER_EXPORT___.push([module.i, "@font-face {\n    font-family: 'Open Sans';\n    src: url('OpenSans-SemiBold.woff2') format('woff2'),\n        url('OpenSans-SemiBold.woff') format('woff');\n    font-weight: 600;\n    font-style: normal;\n}\n\n@font-face {\n    font-family: 'Open Sans';\n    src: url('OpenSans-Regular.woff2') format('woff2'),\n        url('OpenSans-Regular.woff') format('woff');\n    font-weight: 400;\n    font-style: normal;\n}\n\n/* You can add global styles to this file, and also import other style files */\n\nhtml {\n  font-size: 62.5%;\n  box-sizing: border-box;\n  height: 100%;\n  width: 100%;\n  color: var(--black-grey);\n}\n\n*,\n::after,\n::before {\n  box-sizing: inherit;\n}\n\nbody {\n  margin: 0;\n  font-family: 'Open Sans';\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  height: 100%;\n  width: 100%;\n  font-size: 1.6rem;\n}\n\napp-root {\n  width: 100%;\n  height: 100%;\n}\n\nmain {\n  width: 100%;\n  display: flex;\n  flex-direction: column;\n  justify-content: center;\n  background: rgba(255, 255, 255,  1);\n  /*background: rgba(255, 255, 255,  0.95);*/\n}\n\n:root {\n  --mild-opacity: .6;\n  --katie-color: #142680;\n  --white-grey: #F5F7FA;\n  --light-grey: #E5EAF0;\n  --grey-C9D0DA: #C9D0DA;\n  --grey-8D99A7: #8D99A7;\n  --dark-grey: #737F8B;\n  --black-grey: #282B2F;\n}\n\n.white {\n  color: white;\n}\n\n.blue {\n  color: var(--katie-color);\n}\n\n.light-grey {\n  color: var(--light-grey);\n}\n\n.mid-grey {\n  color: var(--grey-8D99A7);\n}\n\n.dark-grey {\n  color: var(--dark-grey);\n}\n\n.black-grey {\n  color: var(--black-grey);\n}\n\n.bg-blue {\n  background-color: var(--katie-color);\n}\n\n.bg-yellow {\n  background-color: #FFBE00;\n}\n\n.border-light-grey {\n  border-color: var(--white-grey);\n}\n\n.mild-opacity {\n  opacity: var(--mild-opacity);\n}\n\n.grey .strong-opacity {\n  opacity: .2;\n}\n\n.mt-8 {\n  margin-top: .8rem;\n}\n\n.mt-12 {\n  margin-top: 1.2rem;\n}\n\n.mt-16 {\n  margin-top: 1.6rem;\n}\n\n.mt-24 {\n  margin-top: 2.4rem;\n}\n\n.mt-48 {\n  margin-top: 4.8rem;\n}\n\n.mt-64 {\n  margin-top: 6.4rem;\n}\n\n.mt-128 {\n  margin-top: 12.8rem;\n}\n\n.mr-8 {\n  margin-right: .8rem;\n}\n\n.mr-12 {\n  margin-right: 1.2rem;\n}\n\n.mr-16 {\n  margin-right: 1.6rem;\n}\n\n.mr-24 {\n  margin-right: 2.4rem;\n}\n\n.mr-32 {\n  margin-right: 3.2rem;\n}\n\n.mr-64 {\n  margin-right: 6.4rem;\n}\n\n.mb-16 {\n  margin-bottom: 1.6rem;\n}\n\n.mb-24 {\n  margin-bottom: 2.4rem;\n}\n\n.mb-48 {\n  margin-bottom: 4.8rem;\n}\n\n.ml-8 {\n  margin-left: 1.2rem;\n}\n\n.ml-12 {\n  margin-left: 1.2rem;\n}\n\n.ml-24 {\n  margin-left: 2.4rem;\n}\n\n.ml-32 {\n  margin-left: 3.2rem;\n}\n\n.ml-48 {\n  margin-left: 3.2rem;\n}\n\n.p-24 {\n  padding: 2.4rem;\n}\n\n.p-64 {\n  padding: 6.4rem;\n}\n\n.pt-24 {\n  padding-top: 2.4rem;\n}\n\n.pt-48 {\n  padding-top: 4.8rem;\n}\n\n.pt-64 {\n  padding-top: 6.4rem;\n}\n\n.pr-24 {\n  padding-right: 2.4rem;\n}\n\n.pr-64 {\n  padding-right: 6.4rem;\n}\n\n.pb-24 {\n  padding-bottom: 2.4rem;\n}\n\n.pb-48 {\n  padding-bottom: 4.8rem;\n}\n\n.pl-24 {\n  padding-left: 2.4rem;\n}\n\n.large {\n  width: 100%;\n  max-width: 85.1rem;\n}\n\n.row-4 {\n  max-width: 32.8rem;\n}\n\n.row-5 {\n  max-width: 41.6rem;\n}\n\n.row-6 {\n  max-width: 50.4rem;\n}\n\nheader {\n  /*background-color: white;*/\n/*\n  background: url(\"assets/img/background_05_4.svg\");\n  min-height: 15%;\n*/\n  /*opacity: .25;*/\n\n  width: 100%;\n  display: flex;\n  flex-direction: column;\n  /*border-bottom: .2rem solid var(--white-grey);*/\n}\n\n.main-header {\n  padding: 2.4rem 6.4rem;\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  /*opacity: .25;*/\n}\n\n.sub-header {\n  display: flex;\n  padding: 2.4rem 6.4rem;\n  width: 100%;\n  /*background-color: var(--white-grey);*/\n  background: rgba(255, 255, 255,  .9);\n  align-items: center;\n}\n\nfooter {\n  width: 100%;\n  padding: 2.4rem 6.4rem;\n  background-color: var(--white-grey);\n  /*background: rgba(255, 255, 255,  .9);*/\n  display: flex;\n  align-items: center;\n  justify-content: space-between;\n}\n\nfooter div {\n  display: flex;\n  flex-direction: row;\n  align-items: center;\n}\n\np {\n  margin: 0;\n}\n\na {\n  text-decoration: none;\n}\n\n.minion {\n  font-size: 1.4rem;\n}\n\nh1 {\n  font-size: 6.4rem;\n  margin: 0;\n}\n\nh2 {\n  margin: 0;\n  font-size: 3.2rem;\n  font-weight: 600;\n}\n\nh3 {\n  margin: 0;\n  font-weight: 600;\n}\n\nhr {\n  display: block;\n  border: 0;\n  border-top: .2rem solid;\n  border-color: var(--white-grey);\n  padding: 0;\n  margin: 0;\n}\n\n.min-padding {\n  padding: 0 6.4rem;\n  width: 100%;\n}\n\n.table {\n  width: 100%;\n  display: flex;\n  flex-wrap: wrap;\n}\n\n.table p {\n  margin-top: 2.4rem;\n}\n\n.row {\n  width: 100%;\n  display: flex;\n  flex-direction: row;\n  justify-content: space-between;\n}\n\n.row .cell:first-of-type {\n  margin-right: 6.4rem;\n}\n\n.cell {\n  max-width: 41.5rem;\n  margin-top: 6.4rem;\n}\n\n.overlay {\n  position: fixed;\n  top: 0px;\n  left: 0px;\n  width: 100%;\n  height: 100%;\n\n  justify-content: center;\n  align-items: center;\n  z-index: 1000; /* INFO: Make sure the overlay is above everything */\n\n  background-color: rgb(0, 0, 0, 0.4);\n}\n\n.overlay input {\n  max-width: 35rem;\n  width: 100%;\n}\n\n.close-overlay {\n  display: none;\n  opacity: 0;\n}\n\n.open-overlay {\n  display: flex;\n  opacity: 1;\n}\n\n.resubmit-form {\n  background-color: white;\n  padding: 6.4rem;\n  border-radius: 2.4rem;\n  display: flex;\n  flex-direction: column;\n  max-width: 44.5rem;\n}\n\n/* see more at @media only screen and (max-width: 1080px) */\n\n.double-row {}\n\ntextarea {\n  margin: 0;\n  border-radius: .8rem;\n  border: .2rem solid var(--grey-C9D0DA);\n  resize: none;\n  font-size: inherit;\n  width: 100%;\n  height: 20rem;\n}\n\ntextarea:focus {\n  outline: 0;\n  border: .2rem solid var(--katie-color);\n}\n\nul {\n  padding: 0;\n  margin: 0;\n  list-style: none;\n}\n\nbutton {\n  font-size: 1.6rem;\n  border: none;\n  cursor: pointer;\n  margin: 0;\n  padding-left: 0;\n  background: transparent;\n  transition: opacity .5s;\n}\n\n.solid-button {\n  padding: 0.8rem 3.2rem;\n  border-radius: 2.55rem;\n  transition: opacity .5s;\n}\n\n.solid-button:hover {\n  opacity: var(--mild-opacity);\n}\n\n.text-button {\n  font-weight: 600;\n  cursor: pointer;\n  transition: opacity .5s;\n}\n\n.text-button:hover {\n  opacity: .6;\n}\n\n.text-btn-icon {\n  display: flex;\n  align-items: center;\n  font-weight: 600;\n  cursor: pointer;\n  transition: opacity .5s;\n}\n\n.text-btn-icon:hover {\n  opacity: .6;\n}\n\n.fw-600 {\n  font-weight: 600;\n}\n\n.flex {\n  display: flex;\n}\n\n.flex-wrap {\n  flex-wrap: wrap;\n}\n\n.flex-column {\n  flex-direction: column;\n}\n\n.justify-center {\n  display: flex;\n  justify-content: center;\n}\n\n.align-center {\n  display: flex;\n  align-items: center;\n}\n\n.space-between {\n  display: flex;\n  justify-content: space-between;\n}\n\n.text-align-center {\n  text-align: center;\n}\n\n.text-align-left {\n  text-align: left;\n}\n\n.alt-bg {\n  border-radius: 2.4rem;\n  background-color: #F3F4F8;\n}\n\ninput {\n  outline: none;\n  border-radius: 0;\n  font-size: inherit;\n  padding: 0;\n  margin: 0;\n}\n\nselect {\n  outline: none;\n  border-radius: 0;\n  font-size: inherit;\n  padding: 0;\n  margin: 0;\n}\n\n.form-field {\n  padding: 1.4rem 1.6rem;\n  border: .2rem solid var(--light-grey);\n  border-radius: .8rem;\n}\n\n.form-field:focus {\n  outline: 0;\n  border: .2rem solid var(--katie-color);\n}\n\n.icon-form-field {\n  border-radius: .8rem;\n  display: flex;\n  border: .2rem solid var(--light-grey);\n  background-color: white;\n}\n\n.icon-form-field:focus-within {\n  border: .2rem solid var(--katie-color);\n}\n\n.icon-form-field input {\n  padding: 1.4rem 1.6rem;\n  border: none;\n  border-radius: .8rem;\n}\n\n.dd {\n  position: relative;\n}\n\n.dd-content {\n  padding: 2.4rem;\n  box-shadow: 0px 0px 12px 0px var(--light-grey);\n  border-radius: .8rem;\n  background-color: white;\n  position: absolute;\n  text-align: center;\n  z-index: 2;\n}\n\n.transform-translateY-100 {\n  transform: translateY(-100%);\n}\n\n.transform-translateX50 {\n  transform: translateX(50%);\n}\n\n.float-r {\n  float: right;\n}\n\n#account_dd {\n  top: 11.2rem;\n  transform: translateX(calc(-100% + 3.2rem));\n}\n\n#account_dd hr {\n  margin: 2.4rem 0;\n  border-top: solid .2rem var(--light-grey);\n}\n\n.closed-dd {\n  display: none;\n}\n\n.opened-dd {\n  display: block;\n}\n\n.container {\n  width: 100%;\n  max-width: 103.2rem;\n}\n\n.text-container {\n  width: 100%;\n  max-width: 57.5rem;\n}\n\n.icon {\n  border-radius: 50%;\n  width: 4rem;\n  height: 4rem;\n  padding: 0;\n  display: flex;\n  justify-content: center;\n  align-content: center;\n  transition: background-color .5s;\n}\n\n.icon:hover {\n  background-color: var(--light-grey);\n}\n\n.side-nav {\n  width: 27rem;\n  height: 100%;\n  margin-right: 2.4rem;\n}\n\n.sidenav-item {\n  padding: 1.2rem 2.4rem;\n  display: flex;\n  align-items: center;\n}\n\n.sidenav-item a {\n  font-weight: 600;\n  color: var(--black-grey);\n}\n\n.sidenav-item.active {\n  border-radius: .8rem;\n  background-color: rgba(20, 38, 128, 0.1);\n}\n\n.sidenav-item.active a {\n  color: var(--katie-color);\n}\n\n.toolbar {\n  display: flex;\n  align-items: center;\n  padding: 0 1.2rem 2.4rem;\n  border-bottom: .2rem solid var(--white-grey);\n}\n\n.question {\n  position: relative;\n  display: flex;\n  overflow: visible;\n  padding: 2.4rem 1.2rem;\n  background-color: var(--white-grey);\n  justify-content: space-between;\n  align-items: center;\n  border-bottom: .1rem solid var(--light-grey);\n  cursor: pointer;\n}\n\n.question:hover {\n  box-shadow: 1px 1px 12px 0px var(--light-grey);\n  -webkit-box-shadow: 1px 1px 12px 0px var(--light-grey);\n  -moz-box-shadow: 1px 1px 12px 0px var(--light-grey);\n  /*z-index so shadow is on top of other elements*/\n  z-index: 1;\n}\n\n.question a {\n  color: var(--black-grey);\n}\n\n@media only screen and (max-width: 1129px) {\n  .subheader .div1 {\n    width: 50%;\n    align-items: center;\n    padding-bottom: 2.4rem;\n  }\n\n  .subheader .div3 {\n    width: 50%;\n    align-items: center;\n    padding-bottom: 2.4rem;\n  }\n\n  .div1 {\n    order: 1;\n    display: flex;\n    justify-content: flex-start;\n  }\n\n  .div2 {\n    align-items: center;\n    justify-content: center;\n    order: 3;\n  }\n\n  .div3 {\n    order: 2;\n    display: flex;\n    justify-content: flex-end;\n  }\n}\n\n@media only screen and (max-width: 1080px) {\n  .double-row div:first-of-type {\n    margin-bottom: 6.4rem;\n  }\n\n  .min-padding {\n    padding: 0 2.4rem;\n  }\n\n  .main-header {\n    padding: 2.4rem;\n  }\n\n  .sub-header {\n    padding: 2.4rem;\n  }\n\n  footer {\n    padding: 2.4rem;\n  }\n\n  #account-dd {\n    right: 2.4rem;\n  }\n}\n\n@media only screen and (max-width: 848px) {\n  .table {\n    width: 100%;\n    display: flex;\n    flex-wrap: wrap;\n  }\n\n  .row {\n    flex-direction: column;\n  }\n\n  .row .cell:first-of-type {\n    margin-right: auto;\n  }\n\n  .cell {\n    max-width: 41.5rem;\n    margin: 6.4rem auto 0;\n  }\n\n  .alt-bg {\n    border-radius: 0;\n  }\n\n  .overlay {\n    background-color: white;\n  }\n}\n\n/* iPhone 8: 375 x 667*/\n\n@media only screen and (max-width: 375px) {\n  h1 {\n    font-size: 4.8rem;\n  }\n\n  h2 {\n    font-size: 2.4rem;\n  }\n}\n", "",{"version":3,"sources":["webpack://src/assets/scss/modules/_fonts.scss","webpack://src/styles.css"],"names":[],"mappings":"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","sourcesContent":["@font-face {\n    font-family: 'Open Sans';\n    src: url('../../fonts/SemiBold/OpenSans-SemiBold.woff2') format('woff2'),\n        url('../../fonts/SemiBold/OpenSans-SemiBold.woff') format('woff');\n    font-weight: 600;\n    font-style: normal;\n}\n\n@font-face {\n    font-family: 'Open Sans';\n    src: url('../../fonts/Regular/OpenSans-Regular.woff2') format('woff2'),\n        url('../../fonts/Regular/OpenSans-Regular.woff') format('woff');\n    font-weight: 400;\n    font-style: normal;\n}\n","@import 'assets/scss/modules/_fonts.scss';\n\n/* You can add global styles to this file, and also import other style files */\nhtml {\n  font-size: 62.5%;\n  box-sizing: border-box;\n  height: 100%;\n  width: 100%;\n  color: var(--black-grey);\n}\n\n*,\n::after,\n::before {\n  box-sizing: inherit;\n}\n\nbody {\n  margin: 0;\n  font-family: 'Open Sans';\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  height: 100%;\n  width: 100%;\n  font-size: 1.6rem;\n}\n\napp-root {\n  width: 100%;\n  height: 100%;\n}\n\nmain {\n  width: 100%;\n  display: flex;\n  flex-direction: column;\n  justify-content: center;\n  background: rgba(255, 255, 255,  1);\n  /*background: rgba(255, 255, 255,  0.95);*/\n}\n\n:root {\n  --mild-opacity: .6;\n  --katie-color: #142680;\n  --white-grey: #F5F7FA;\n  --light-grey: #E5EAF0;\n  --grey-C9D0DA: #C9D0DA;\n  --grey-8D99A7: #8D99A7;\n  --dark-grey: #737F8B;\n  --black-grey: #282B2F;\n}\n\n.white {\n  color: white;\n}\n\n.blue {\n  color: var(--katie-color);\n}\n\n.light-grey {\n  color: var(--light-grey);\n}\n\n.mid-grey {\n  color: var(--grey-8D99A7);\n}\n\n.dark-grey {\n  color: var(--dark-grey);\n}\n\n.black-grey {\n  color: var(--black-grey);\n}\n\n.bg-blue {\n  background-color: var(--katie-color);\n}\n\n.bg-yellow {\n  background-color: #FFBE00;\n}\n\n.border-light-grey {\n  border-color: var(--white-grey);\n}\n\n.mild-opacity {\n  opacity: var(--mild-opacity);\n}\n\n.grey .strong-opacity {\n  opacity: .2;\n}\n\n.mt-8 {\n  margin-top: .8rem;\n}\n\n.mt-12 {\n  margin-top: 1.2rem;\n}\n\n.mt-16 {\n  margin-top: 1.6rem;\n}\n\n.mt-24 {\n  margin-top: 2.4rem;\n}\n\n.mt-48 {\n  margin-top: 4.8rem;\n}\n\n.mt-64 {\n  margin-top: 6.4rem;\n}\n\n.mt-128 {\n  margin-top: 12.8rem;\n}\n\n.mr-8 {\n  margin-right: .8rem;\n}\n\n.mr-12 {\n  margin-right: 1.2rem;\n}\n\n.mr-16 {\n  margin-right: 1.6rem;\n}\n\n.mr-24 {\n  margin-right: 2.4rem;\n}\n\n.mr-32 {\n  margin-right: 3.2rem;\n}\n\n.mr-64 {\n  margin-right: 6.4rem;\n}\n\n.mb-16 {\n  margin-bottom: 1.6rem;\n}\n\n.mb-24 {\n  margin-bottom: 2.4rem;\n}\n\n.mb-48 {\n  margin-bottom: 4.8rem;\n}\n\n.ml-8 {\n  margin-left: 1.2rem;\n}\n\n.ml-12 {\n  margin-left: 1.2rem;\n}\n\n.ml-24 {\n  margin-left: 2.4rem;\n}\n\n.ml-32 {\n  margin-left: 3.2rem;\n}\n\n.ml-48 {\n  margin-left: 3.2rem;\n}\n\n.p-24 {\n  padding: 2.4rem;\n}\n\n.p-64 {\n  padding: 6.4rem;\n}\n\n.pt-24 {\n  padding-top: 2.4rem;\n}\n\n.pt-48 {\n  padding-top: 4.8rem;\n}\n\n.pt-64 {\n  padding-top: 6.4rem;\n}\n\n.pr-24 {\n  padding-right: 2.4rem;\n}\n\n.pr-64 {\n  padding-right: 6.4rem;\n}\n\n.pb-24 {\n  padding-bottom: 2.4rem;\n}\n\n.pb-48 {\n  padding-bottom: 4.8rem;\n}\n\n.pl-24 {\n  padding-left: 2.4rem;\n}\n\n.large {\n  width: 100%;\n  max-width: 85.1rem;\n}\n\n.row-4 {\n  max-width: 32.8rem;\n}\n\n.row-5 {\n  max-width: 41.6rem;\n}\n\n.row-6 {\n  max-width: 50.4rem;\n}\n\nheader {\n  /*background-color: white;*/\n/*\n  background: url(\"assets/img/background_05_4.svg\");\n  min-height: 15%;\n*/\n  /*opacity: .25;*/\n\n  width: 100%;\n  display: flex;\n  flex-direction: column;\n  /*border-bottom: .2rem solid var(--white-grey);*/\n}\n\n.main-header {\n  padding: 2.4rem 6.4rem;\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  /*opacity: .25;*/\n}\n\n.sub-header {\n  display: flex;\n  padding: 2.4rem 6.4rem;\n  width: 100%;\n  /*background-color: var(--white-grey);*/\n  background: rgba(255, 255, 255,  .9);\n  align-items: center;\n}\n\nfooter {\n  width: 100%;\n  padding: 2.4rem 6.4rem;\n  background-color: var(--white-grey);\n  /*background: rgba(255, 255, 255,  .9);*/\n  display: flex;\n  align-items: center;\n  justify-content: space-between;\n}\n\nfooter div {\n  display: flex;\n  flex-direction: row;\n  align-items: center;\n}\n\np {\n  margin: 0;\n}\n\na {\n  text-decoration: none;\n}\n\n.minion {\n  font-size: 1.4rem;\n}\n\nh1 {\n  font-size: 6.4rem;\n  margin: 0;\n}\n\nh2 {\n  margin: 0;\n  font-size: 3.2rem;\n  font-weight: 600;\n}\n\nh3 {\n  margin: 0;\n  font-weight: 600;\n}\n\nhr {\n  display: block;\n  border: 0;\n  border-top: .2rem solid;\n  border-color: var(--white-grey);\n  padding: 0;\n  margin: 0;\n}\n\n.min-padding {\n  padding: 0 6.4rem;\n  width: 100%;\n}\n\n.table {\n  width: 100%;\n  display: flex;\n  flex-wrap: wrap;\n}\n\n.table p {\n  margin-top: 2.4rem;\n}\n\n.row {\n  width: 100%;\n  display: flex;\n  flex-direction: row;\n  justify-content: space-between;\n}\n\n.row .cell:first-of-type {\n  margin-right: 6.4rem;\n}\n\n.cell {\n  max-width: 41.5rem;\n  margin-top: 6.4rem;\n}\n\n.overlay {\n  position: fixed;\n  top: 0px;\n  left: 0px;\n  width: 100%;\n  height: 100%;\n\n  justify-content: center;\n  align-items: center;\n  z-index: 1000; /* INFO: Make sure the overlay is above everything */\n\n  background-color: rgb(0, 0, 0, 0.4);\n}\n\n.overlay input {\n  max-width: 35rem;\n  width: 100%;\n}\n\n.close-overlay {\n  display: none;\n  opacity: 0;\n}\n\n.open-overlay {\n  display: flex;\n  opacity: 1;\n}\n\n.resubmit-form {\n  background-color: white;\n  padding: 6.4rem;\n  border-radius: 2.4rem;\n  display: flex;\n  flex-direction: column;\n  max-width: 44.5rem;\n}\n\n/* see more at @media only screen and (max-width: 1080px) */\n.double-row {}\n\ntextarea {\n  margin: 0;\n  border-radius: .8rem;\n  border: .2rem solid var(--grey-C9D0DA);\n  resize: none;\n  font-size: inherit;\n  width: 100%;\n  height: 20rem;\n}\n\ntextarea:focus {\n  outline: 0;\n  border: .2rem solid var(--katie-color);\n}\n\nul {\n  padding: 0;\n  margin: 0;\n  list-style: none;\n}\n\nbutton {\n  font-size: 1.6rem;\n  border: none;\n  cursor: pointer;\n  margin: 0;\n  padding-left: 0;\n  background: transparent;\n  transition: opacity .5s;\n}\n\n.solid-button {\n  padding: 0.8rem 3.2rem;\n  border-radius: 2.55rem;\n  transition: opacity .5s;\n}\n\n.solid-button:hover {\n  opacity: var(--mild-opacity);\n}\n\n.text-button {\n  font-weight: 600;\n  cursor: pointer;\n  transition: opacity .5s;\n}\n\n.text-button:hover {\n  opacity: .6;\n}\n\n.text-btn-icon {\n  display: flex;\n  align-items: center;\n  font-weight: 600;\n  cursor: pointer;\n  transition: opacity .5s;\n}\n\n.text-btn-icon:hover {\n  opacity: .6;\n}\n\n.fw-600 {\n  font-weight: 600;\n}\n\n.flex {\n  display: flex;\n}\n\n.flex-wrap {\n  flex-wrap: wrap;\n}\n\n.flex-column {\n  flex-direction: column;\n}\n\n.justify-center {\n  display: flex;\n  justify-content: center;\n}\n\n.align-center {\n  display: flex;\n  align-items: center;\n}\n\n.space-between {\n  display: flex;\n  justify-content: space-between;\n}\n\n.text-align-center {\n  text-align: center;\n}\n\n.text-align-left {\n  text-align: left;\n}\n\n.alt-bg {\n  border-radius: 2.4rem;\n  background-color: #F3F4F8;\n}\n\ninput {\n  outline: none;\n  border-radius: 0;\n  font-size: inherit;\n  padding: 0;\n  margin: 0;\n}\n\nselect {\n  outline: none;\n  border-radius: 0;\n  font-size: inherit;\n  padding: 0;\n  margin: 0;\n}\n\n.form-field {\n  padding: 1.4rem 1.6rem;\n  border: .2rem solid var(--light-grey);\n  border-radius: .8rem;\n}\n\n.form-field:focus {\n  outline: 0;\n  border: .2rem solid var(--katie-color);\n}\n\n.icon-form-field {\n  border-radius: .8rem;\n  display: flex;\n  border: .2rem solid var(--light-grey);\n  background-color: white;\n}\n\n.icon-form-field:focus-within {\n  border: .2rem solid var(--katie-color);\n}\n\n.icon-form-field input {\n  padding: 1.4rem 1.6rem;\n  border: none;\n  border-radius: .8rem;\n}\n\n.dd {\n  position: relative;\n}\n\n.dd-content {\n  padding: 2.4rem;\n  box-shadow: 0px 0px 12px 0px var(--light-grey);\n  border-radius: .8rem;\n  background-color: white;\n  position: absolute;\n  text-align: center;\n  z-index: 2;\n}\n\n.transform-translateY-100 {\n  transform: translateY(-100%);\n}\n\n.transform-translateX50 {\n  transform: translateX(50%);\n}\n\n.float-r {\n  float: right;\n}\n\n#account_dd {\n  top: 11.2rem;\n  transform: translateX(calc(-100% + 3.2rem));\n}\n\n#account_dd hr {\n  margin: 2.4rem 0;\n  border-top: solid .2rem var(--light-grey);\n}\n\n.closed-dd {\n  display: none;\n}\n\n.opened-dd {\n  display: block;\n}\n\n.container {\n  width: 100%;\n  max-width: 103.2rem;\n}\n\n.text-container {\n  width: 100%;\n  max-width: 57.5rem;\n}\n\n.icon {\n  border-radius: 50%;\n  width: 4rem;\n  height: 4rem;\n  padding: 0;\n  display: flex;\n  justify-content: center;\n  align-content: center;\n  transition: background-color .5s;\n}\n\n.icon:hover {\n  background-color: var(--light-grey);\n}\n\n.side-nav {\n  width: 27rem;\n  height: 100%;\n  margin-right: 2.4rem;\n}\n\n.sidenav-item {\n  padding: 1.2rem 2.4rem;\n  display: flex;\n  align-items: center;\n}\n\n.sidenav-item a {\n  font-weight: 600;\n  color: var(--black-grey);\n}\n\n.sidenav-item.active {\n  border-radius: .8rem;\n  background-color: rgba(20, 38, 128, 0.1);\n}\n\n.sidenav-item.active a {\n  color: var(--katie-color);\n}\n\n.toolbar {\n  display: flex;\n  align-items: center;\n  padding: 0 1.2rem 2.4rem;\n  border-bottom: .2rem solid var(--white-grey);\n}\n\n.question {\n  position: relative;\n  display: flex;\n  overflow: visible;\n  padding: 2.4rem 1.2rem;\n  background-color: var(--white-grey);\n  justify-content: space-between;\n  align-items: center;\n  border-bottom: .1rem solid var(--light-grey);\n  cursor: pointer;\n}\n\n.question:hover {\n  box-shadow: 1px 1px 12px 0px var(--light-grey);\n  -webkit-box-shadow: 1px 1px 12px 0px var(--light-grey);\n  -moz-box-shadow: 1px 1px 12px 0px var(--light-grey);\n  /*z-index so shadow is on top of other elements*/\n  z-index: 1;\n}\n\n.question a {\n  color: var(--black-grey);\n}\n\n@media only screen and (max-width: 1129px) {\n  .subheader .div1 {\n    width: 50%;\n    align-items: center;\n    padding-bottom: 2.4rem;\n  }\n\n  .subheader .div3 {\n    width: 50%;\n    align-items: center;\n    padding-bottom: 2.4rem;\n  }\n\n  .div1 {\n    order: 1;\n    display: flex;\n    justify-content: flex-start;\n  }\n\n  .div2 {\n    align-items: center;\n    justify-content: center;\n    order: 3;\n  }\n\n  .div3 {\n    order: 2;\n    display: flex;\n    justify-content: flex-end;\n  }\n}\n\n@media only screen and (max-width: 1080px) {\n  .double-row div:first-of-type {\n    margin-bottom: 6.4rem;\n  }\n\n  .min-padding {\n    padding: 0 2.4rem;\n  }\n\n  .main-header {\n    padding: 2.4rem;\n  }\n\n  .sub-header {\n    padding: 2.4rem;\n  }\n\n  footer {\n    padding: 2.4rem;\n  }\n\n  #account-dd {\n    right: 2.4rem;\n  }\n}\n\n@media only screen and (max-width: 848px) {\n  .table {\n    width: 100%;\n    display: flex;\n    flex-wrap: wrap;\n  }\n\n  .row {\n    flex-direction: column;\n  }\n\n  .row .cell:first-of-type {\n    margin-right: auto;\n  }\n\n  .cell {\n    max-width: 41.5rem;\n    margin: 6.4rem auto 0;\n  }\n\n  .alt-bg {\n    border-radius: 0;\n  }\n\n  .overlay {\n    background-color: white;\n  }\n}\n\n/* iPhone 8: 375 x 667*/\n@media only screen and (max-width: 375px) {\n  h1 {\n    font-size: 4.8rem;\n  }\n\n  h2 {\n    font-size: 2.4rem;\n  }\n}\n"],"sourceRoot":""}]);
// Exports
/* harmony default export */ __webpack_exports__["default"] = (___CSS_LOADER_EXPORT___);


/***/ })

},[[3,"runtime"]]]);
//# sourceMappingURL=styles-es2015.js.map