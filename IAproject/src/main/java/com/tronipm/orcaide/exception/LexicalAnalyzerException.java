/*
 * Copyright 2018 Paulo Mateus da Silva.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tronipm.orcaide.exception;

import com.tronipm.orcaide.model.TokenPreProcessamento;

/**
 *
 * @author Matt
 */
public class LexicalAnalyzerException extends Exception {

    public LexicalAnalyzerException(String message) {
        super(message);
    }

    public LexicalAnalyzerException(TokenPreProcessamento l) {
        super("Unknow token '" + l.lexeme + "' at line " + l.line + " and position " + l.position + ".");
    }
}
