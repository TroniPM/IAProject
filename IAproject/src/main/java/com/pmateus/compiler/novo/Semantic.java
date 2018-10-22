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
package com.pmateus.compiler.novo;

import com.pmateus.compiler.CompiladorToken;
import com.pmateus.compiler.Util;
import com.pmateus.compiler.exception.SemanticAnalyzerException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Matt
 */
public class Semantic {

    private static Semantic semantic = null;
    private ArrayList<CompiladorToken> tokens = new ArrayList<>();

    public static Semantic getInstance() {
        if (semantic == null) {
            semantic = new Semantic();
        }

        return semantic;
    }

    public ArrayList<CompiladorToken> getTokens() {
        return tokens;
    }

    public boolean init(String source) throws SemanticAnalyzerException {
        tokens = new ArrayList<>();
        String newSource = spaces(source);

        String[] cmd = newSource.split(";");

        cmd = spaces(cmd);

        for (int cm = 0; cm < cmd.length; cm++) {
            char[] itens = cmd[cm].toCharArray();
            for (int i = 0; i < itens.length; i++) {
                boolean flag = false;
                if (itens[i] == ')') {
                    inner:
                    for (int j = i - 1; j >= 0; j--) {
                        if (itens[j] == '(') {
                            //(...) encontrado
                            String particula = cmd[cm].substring(j, i + 1);
                            CompiladorToken token = new CompiladorToken(0, 0, particula);
                            tokens.add(token);
//                            System.out.println("VAI TROCAR: " + token.label + " >> " + token.id);
                            cmd[cm] = cmd[cm].replace(token.label, token.id);
                            flag = true;
                            break inner;
                        }
                    }
//                    System.out.println("ATUAL > " + cmd[cm]);
                    if (flag) {
                        itens = cmd[cm].toCharArray();
                        i = -1;//RESETANDO
                    }
                }
            }
        }

        cmd = substituir2(cmd, Util.NOT);

        /**
         * TODO ordem de resolução. Adicionar campos abaixo
         */
//    public static final String VALUE = "value";
//    public static final String MIN = "min";
//    public static final String MAX = "max";
//    public static final String EXACTLY = "exactly";
//        cmd = substituir3(cmd, Util.EXACTLY);
//        cmd = substituir3(cmd, Util.VALUE);
//        cmd = substituir3(cmd, Util.MIN);
//        cmd = substituir3(cmd, Util.MAX);
        cmd = substituir3(cmd, Util.SOME);
        cmd = substituir3(cmd, Util.ALL);
        cmd = substituir3(cmd, Util.ONLY);
        cmd = substituir3(cmd, Util.AND);
        cmd = substituir3(cmd, Util.OR);
        cmd = substituir3(cmd, Util.ISA);
        cmd = substituir3(cmd, Util.EQUIVALENT);
        cmd = substituir3(cmd, Util.THAT);
        System.out.println(Arrays.toString(cmd));
        System.out.println("*******************************************");

        StringBuilder toPrint = new StringBuilder();
        for (CompiladorToken token : tokens) {

            StringTokenizer st = new StringTokenizer(token.label.replaceAll(" +", " ").trim());
            String last = "";
            boolean isNOT = false;
            boolean primeiro = true;
            boolean lastReservado = false;
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                boolean reservado = false;

                if (str.equals("(") || str.equals(")")) {//Verificação já foi feita anteriormente
                    continue;
                }

                if (str.equalsIgnoreCase(Util.NOT)) {
                    isNOT = true;
                    continue;
                }

                for (String out : Util.COMMANDS_LIST) {
                    if (str.equalsIgnoreCase(out)) {
                        reservado = true;
                        break;
                    }
                }

                if ((reservado || Character.isDigit(str.charAt(0))) && isNOT) {//Se for uma NEGACAO, não pode vir palavra-reservada/numero depois, apenas classes/propriedade
                    throw new SemanticAnalyzerException("'NOT' operator can only be used with classes and properties at command '" + token.label.replaceAll(" +", " ").trim() + "', at line " + token.line + ".");
                }

                if (lastReservado && reservado) {
                    throw new SemanticAnalyzerException("Unexpected tokens at '" + last + " " + str + "', command '" + token.label.replaceAll(" +", " ").trim() + "'.");
                } else if (!lastReservado && !reservado && !primeiro) {
                    throw new SemanticAnalyzerException("Unexpected tokens at '" + last + " " + str + "', command '" + token.label.replaceAll(" +", " ").trim() + "'.");
                }

                if (reservado) {
                    lastReservado = true;
                } else {
                    lastReservado = false;
                }
                last = str;
                primeiro = false;
            }

            //----------------------------
            toPrint.append(token.string());
            toPrint.append("\r\n");
        }
        System.out.println(toPrint.toString());

        return true;
    }

    private String[] substituir2(String[] cmd, String PATTERN) {
        for (int linha = 0; linha < cmd.length; linha++) {
            if (cmd[linha].trim().isEmpty()) {
                continue;
            }
            StringTokenizer st = new StringTokenizer(cmd[linha]);

            boolean operador = false;
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.equals(PATTERN)) {
                    operador = true;
                    continue;
                }

                if (operador) {
                    String particula = PATTERN + " " + s;
                    CompiladorToken token = new CompiladorToken(0, 0, particula);
                    tokens.add(token);
//                    System.out.println("VAI TROCAR: " + token.label + " >> " + token.id);
                    cmd[linha] = cmd[linha].replace(token.label, token.id);

                    operador = false;
                }
            }
        }

        return cmd;
    }

    private String[] substituir3(String[] cmd, String PATTERN) {
        for (int linha = 0; linha < cmd.length; linha++) {
            if (cmd[linha].trim().isEmpty()) {
                continue;
            }
            StringTokenizer st = new StringTokenizer(cmd[linha]);

            String last = "";
            boolean operador = false;
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                boolean flag = false;
                if (s.equals(PATTERN)) {
                    flag = true;//mudo para true apenas para não entrar nos 2 ifs seguintes nessa iteração
                    operador = true;
                }

                if (operador && !flag) {
                    String particula = last + " " + PATTERN + " " + s;
                    CompiladorToken token = new CompiladorToken(0, 0, particula);
                    tokens.add(token);
//                    System.out.println("VAI TROCAR: " + token.label + " >> " + token.id);
                    cmd[linha] = cmd[linha].replace(token.label, token.id);
//                    return substituir3(cmd, PATTERN);
                }

                if (!flag) {
                    operador = false;
                    last = s;
                }
            }
        }

        return cmd;
    }

    private String spaces(String source) {
        String a = source//.replace("\r\n", " ")
                //.replace("\r", " ")
                //.replace("\n", " ")
                .replace(")", " ) ")
                .replace("(", " ( ")
                .replace(";", " ; ");
        //.trim().replaceAll(" +", " ");

        //Todas as palavras reservadas para lower case
        for (String in : a.split(" ")) {
            for (String out : Util.COMMANDS_LIST) {
                if (in.equalsIgnoreCase(out)) {

                    a = a.replace(" " + in + " ", " " + out + " ");
                }
            }
        }

        return a;
    }

    private String[] spaces(String[] cmd) {
        for (int i = 0; i < cmd.length; i++) {
            cmd[i] = cmd[i].replaceAll(" +", " ").trim();
        }

        return cmd;
    }
}