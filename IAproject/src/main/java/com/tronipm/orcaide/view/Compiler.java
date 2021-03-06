/*
 * Copyright 2016 Paulo Mateus [UFRPE-UAG] <paulomatew@gmail.com>
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
package com.tronipm.orcaide.view;

import com.google.common.base.Joiner;
import com.tronipm.orcaide.controller.ControllerConversor;
import com.tronipm.orcaide.exception.ConversorException;
import com.tronipm.orcaide.exception.LexicalAnalyzerException;
import com.tronipm.orcaide.exception.SemanticAnalyzerException;
import com.tronipm.orcaide.exception.SintaticAnalyzerException;
import com.tronipm.orcaide.controller.ControllerLexical;
import com.tronipm.orcaide.controller.ControllerSemantic;
import com.tronipm.orcaide.controller.ControllerSintatic;
import com.tronipm.orcaide.view.util.LinePainter;
import com.tronipm.orcaide.view.util.TextLineNumber;
import com.tronipm.orcaide.view.util.popupmenu.PopUpMenuAtRightClickNormalEditorListener;
import com.tronipm.orcaide.view.util.popupmenu.PopUpMenuAtRightClickMainEditorListener;
import com.tronipm.orcaide.core.InsertionAnalyser;
import com.tronipm.orcaide.util.Session;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler extends javax.swing.JPanel {

    private JFramePrincipal jFrameMain;
    public UndoManager undoRedoManager;

    public javax.swing.JTextPane editor;
    private DefaultStyledDocument doc;

    private final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    public void destroy() {
        jFrameMain = null;
        undoRedoManager = null;
    }

    public String getSelectedText() {
        if (jTextPane1.getSelectedText() != null) {
            return jTextPane1.getSelectedText();
        }
        if (jTextPane2.getSelectedText() != null) {
            return jTextPane2.getSelectedText();
        }

        return "";
    }

    /**
     * Creates new form Add
     *
     * @param frameMain
     */
    public Compiler(JFramePrincipal frameMain) {
        this.jFrameMain = frameMain;
        initEditor();
        initComponents();

        TextLineNumber tln = new TextLineNumber(jTextPane1);
        jScrollPane1.setRowHeaderView(tln);
        LinePainter lp = new LinePainter(jTextPane1, Color.decode("#eeeeee"));

        initRedoUndoScheme();

        editor = jTextPane1;//Dou uma referência

        jTextPane1.addMouseListener(new PopUpMenuAtRightClickMainEditorListener(jTextPane1));
        jTextPane2.addMouseListener(new PopUpMenuAtRightClickNormalEditorListener(jTextPane2));
    }

    private void initRedoUndoScheme() {
        //Adicionando listner pro ctrl+z do textarea de inserção
        undoRedoManager = new UndoManager();
        Document doc = jTextPane1.getDocument();
        doc.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoRedoManager.addEdit(e.getEdit());
            }
        });
        InputMap im = jTextPane1.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = jTextPane1.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");
        am.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoRedoManager.canUndo()) {
                        undoRedoManager.undo();
                    }
                } catch (CannotUndoException exp) {
                    exp.printStackTrace();
                }
            }
        });
        am.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoRedoManager.canRedo()) {
                        undoRedoManager.redo();
                    }
                } catch (CannotUndoException exp) {
                    exp.printStackTrace();
                }
            }
        });
    }

    private void initEditor() {

        final StyleContext cont = StyleContext.getDefaultStyleContext();
        final AttributeSet attr_blue = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.BLUE);
        final AttributeSet attr_green = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.GREEN);
        final AttributeSet attrBlack = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.BLACK);

        final String joined = Joiner.on("|").skipNulls().join(InsertionAnalyser.commands_list);

        doc = new DefaultStyledDocument() {
            @Override
            public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offset, str, a);

                String text = getText(0, getLength());
                int before = findLastNonWordChar(text, offset);
                if (before < 0) {
                    before = 0;
                }
                int after = findFirstNonWordChar(text, offset + str.length());
                int wordL = before;
                int wordR = before;

                while (wordR <= after) {
                    if (wordR == after || String.valueOf(text.charAt(wordR)).matches("\\W")) {
                        if (text.substring(wordL, wordR).toLowerCase().matches("(\\W)*(" + joined + ")")) {
                            setCharacterAttributes(wordL, wordR - wordL, attr_blue, false);
                        } else if (text.substring(wordL, wordR).matches("[0-9]+")) {
                            setCharacterAttributes(wordL, wordR - wordL, attr_green, false);
                        } else {
                            setCharacterAttributes(wordL, wordR - wordL, attrBlack, false);
                        }
                        wordL = wordR;
                    }
                    wordR++;
                }
            }

            public void remove(int offs, int len) throws BadLocationException {
                super.remove(offs, len);

                String text = getText(0, getLength());
                int before = findLastNonWordChar(text, offs);
                if (before < 0) {
                    before = 0;
                }
                int after = findFirstNonWordChar(text, offs);
                if (text.substring(before, after).toLowerCase().matches("(\\W)*(" + joined + ")")) {
                    setCharacterAttributes(before, after - before, attr_blue, false);
                } else if (text.substring(before, after).matches("[0-9]+")) {
                    setCharacterAttributes(before, after - before, attr_green, false);
                } else {
                    setCharacterAttributes(before, after - before, attrBlack, false);
                }
            }
        };
    }

    private int findLastNonWordChar(String text, int index) {
        while (--index >= 0) {
            if (String.valueOf(text.charAt(index)).matches("\\W")) {
                break;
            }
        }
        return index;
    }

    private int findFirstNonWordChar(String text, int index) {
        while (index < text.length()) {
            if (String.valueOf(text.charAt(index)).matches("\\W")) {
                break;
            }
            index++;
        }
        return index;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane(doc);

        setMinimumSize(new java.awt.Dimension(0, 0));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/tronipm/orcaide/view/Bundle"); // NOI18N
        jLabel1.setText(bundle.getString("Compiler.jLabel1.text")); // NOI18N

        jButton1.setText(bundle.getString("Compiler.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(bundle.getString("Compiler.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel2.setText(bundle.getString("Compiler.jLabel2.text")); // NOI18N

        jButton4.setText(bundle.getString("Compiler.jButton4.text")); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jTextPane2.setEditable(false);
        jTextPane2.setContentType("text/html"); // NOI18N
        jTextPane2.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane2.setViewportView(jTextPane2);

        jTextPane1.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextPane1.setText(bundle.getString("Compiler.jTextPane1.text")); // NOI18N
        jTextPane1.setToolTipText(bundle.getString("Compiler.jTextPane1.toolTipText")); // NOI18N
        jTextPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextPane1KeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(jTextPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jButton4))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 576, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        jTextPane1.setText("");

        this.jFrameMain.coreApp.owlRepository.undoRedoOntologyManager.printThis();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (Session.isDebbug) {
            java.util.logging.Logger.getLogger(Compiler.class.getName()).log(Level.INFO, "COMPILE BUTTON()");
        }

        try {
            ControllerLexical.getInstance().init(jTextPane1.getText());
            jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#696969\" face=\"\" /><b> Lexical</b> OK.</font><br/>";
            ControllerSintatic.getInstance().init(ControllerLexical.getInstance().getTokens());
            jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#696969\" face=\"\" /><b>  Sintatic</b> OK.</font><br/>";
            ControllerSemantic.getInstance().init(jTextPane1.getText());
            jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#696969\" face=\"\" /><b>   Semantic</b> OK.</font><br/>";

            ControllerConversor.getInstance().init(ControllerSemantic.getInstance().getTokens(), jFrameMain);
            jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#006400\" face=\"\" /><b> Conversor</b> OK.</font><br/><br/>";
//            if (lexical) {
//                jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#696969\" face=\"\" /><b> Lexical</b> OK.</font><br/>";
//                boolean sintatic = Sintatic.getInstance().init(jTextPane1.getText(), jFrameMain);
//
//                if (sintatic) {
//                    jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#696969\" face=\"\" /><b>Sintatic</b> OK.</font><br/>";
//
//                    boolean conversor = Conversor.getInstance().init(Sintatic.getInstance().tokens, jFrameMain);
//                    if (conversor) {
//                        jFrameMain.coreApp.iAnalyser.erroString += "<font color=\"#006400\" face=\"\" /><b> Conversor</b> OK.</font><br/><br/>";
//                    } else {
//                        throw new ConversorException("Conversor couldn't finish the job.");
//                    }
//                    try {
//                        jFrameMain.coreApp.owlRepository.saveState();
//                    } catch (OWLOntologyCreationException ex) {
//                        java.util.logging.Logger.getLogger(InsertionAnalyser.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                } else {
//                    throw new SintaticAnalyzerException("Sintatic analyzer can't verify the code.");
//                }
//            } else {
//                throw new LexicalAnalyzerException("Lexical analyzer can't verify the code.");
//            }
        } catch (ConversorException | SemanticAnalyzerException | LexicalAnalyzerException | SintaticAnalyzerException | OWLOntologyCreationException ex) {
            java.util.logging.Logger.getLogger(Compiler.class.getName()).log(Level.SEVERE, null, ex);
            jFrameMain.coreApp.iAnalyser.erroString += "&nbsp;&nbsp;<font color=\"#ff0000\" face=\"\" /><b>" + ex.getClass().getSimpleName() + "</b>: " + ex.getMessage() + "</font><br/>";
        }

//        jFrameMain.coreApp.onSubmitted("");
        jFrameMain.coreApp.atualizarTelas();
        attLogPanel();
    }//GEN-LAST:event_jButton1ActionPerformed

    private String replaceLineBreaker(String old) {
        return old.replace("\n", "").replace("<br>", "").replace("<br/>", "");
    }

    public void attLogPanel() {
        if (Session.isDebbug) {
            System.out.println(Compiler.class + " attLogPanel()");
        }
        jTextPane2.setText("<html>" + jFrameMain.coreApp.iAnalyser.erroString + "</html>");
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jFrameMain.coreApp.iAnalyser.erroString = "";
        attLogPanel();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jTextPane1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextPane1KeyTyped
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            editor.setText("<html>" + editor.getText() + "<br>" + "</html>");
        }
    }//GEN-LAST:event_jTextPane1KeyTyped


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    // End of variables declaration//GEN-END:variables

}
