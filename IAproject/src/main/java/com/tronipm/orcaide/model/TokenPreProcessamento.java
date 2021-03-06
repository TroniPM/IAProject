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
package com.tronipm.orcaide.model;

/**
 *
 * @author Matt
 */
public class TokenPreProcessamento {

    public TokenEnum type;
    public String lexeme = null;
    public String description1 = null;
    public String description2 = null;
    public String regra = null;
    public int line = 0;
    public int position = 0;

    public boolean wasMapped = false;

    public boolean isNegacao = false;
    public String id;

    public boolean used = false;//para compatibilidade

    @Override
    public TokenPreProcessamento clone() {
        TokenPreProcessamento t = new TokenPreProcessamento();
        t.type = type;
        t.lexeme = lexeme;
        t.description1 = description1;
        t.description2 = description2;
        t.regra = regra;
        t.line = line;
        t.position = position;
//        t.scope = scope;
//        t.other = other;

        return t;
    }

    public TokenPreProcessamento() {
    }

    public TokenPreProcessamento(TokenEnum type) {
        this.type = type;
    }

//    public Token(int type, String lexeme) {
//        this.type = TokenEnum.parse(type);
//        this.lexeme = lexeme;
//    }
//    public Token(int type, String lexeme, String description) {
//        this.type = TokenEnum.parse(type);
//        this.lexeme = lexeme;
//        this.description1 = description;
//    }
    public TokenPreProcessamento(int type, String lexeme, String description) {
        this.type = TokenEnum.parse(type);
        this.lexeme = lexeme;
        this.description1 = description;
//        this.other = other;
    }

    public void print() {
        System.out.println("TOKEN: " + type + " | " + type.getValor());
        System.out.println("LEXEMA: " + lexeme);
        System.out.println("DESCRIÇÃO: " + description1);
        System.out.println("LINHA: " + line);
        System.out.println("POSIÇÃO: " + position);
//        System.out.println("ESCOPO: " + scope);
        System.out.println("REGRA: " + regra);
        System.out.println("--------------------------------");
    }

    public String toString() {
        String a = "";
        a += ("\nTOKEN: " + type);
        a += ("\nLEXEMA: " + lexeme);
        a += ("\nDESCRIÇÃO: " + description1);
        a += ("\nLINHA: " + line);
        a += ("\nPOSIÇÃO: " + position);
//        a += ("\nESCOPO: " + scope);
        a += ("\nREGRA: " + regra);
        a += ("\n--------------------------------");

        return a;
    }

    public String string() {
        String a = "id: " + id + " | ";
        a += "label: " + (isNegacao ? "NOT> " : "") + lexeme;
        return a;
    }
}
