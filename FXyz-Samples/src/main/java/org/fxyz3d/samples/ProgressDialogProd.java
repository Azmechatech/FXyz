/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxyz3d.samples;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author ryzen
 */
public class ProgressDialogProd {
    
    
    public static void main(String[] args) {
        new ProgressDialogProd(new ArrayList<DownloadTask>());
    }

    public ProgressDialogProd(List<DownloadTask> downloadTaskList) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
//                try {
//                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//                } catch (Exception ex) {
//                }

                SwingWorker worker = new SwingWorker() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        for (int index = 0; index < downloadTaskList.size(); index++) {
                            
                            String response=downloadTaskList.get(index).download();
                            setProgress(index);
                        }
                        return null;
                    }

                };

                ProgressDialog.showProgress(null,"www.truegeometry.com","downloading...",downloadTaskList.size(), worker);

              //  System.exit(0);

            }

        });
    }

    public static class ProgressDialog extends JDialog {

        private JLabel message;
        private JLabel subMessage;
        private JProgressBar progressBar;

        public ProgressDialog(Component parent,String messageText,String subMessageText,int max, SwingWorker worker) {

            super(parent == null ? null : SwingUtilities.getWindowAncestor(parent));
            setModal(true);

            ((JComponent)getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

            this.message = new JLabel(messageText);
            subMessage = new JLabel(subMessageText);
            progressBar = new JProgressBar();
            progressBar.setMaximum(max);

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(message, gbc);

            gbc.gridy++;
            add(subMessage, gbc);

            gbc.gridy++;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(progressBar, gbc);

            pack();

            worker.addPropertyChangeListener(new PropertyChangeHandler());
            switch (worker.getState()) {
                case PENDING:
                    worker.execute();
                    break;
            }

        }

        public static void showProgress(Component parent,String messageText,String subMessageText,int max, SwingWorker worker) {

            ProgressDialog dialog = new ProgressDialog(parent,messageText,subMessageText,max, worker);
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);

        }

        public class PropertyChangeHandler implements PropertyChangeListener {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state")) {
                    SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
                    switch (state) {
                        case DONE:
                            dispose();
                            break;
                    }
                } else if (evt.getPropertyName().equals("progress")) {
                    progressBar.setValue((int)evt.getNewValue());
                }
            }

        }

    }
}
