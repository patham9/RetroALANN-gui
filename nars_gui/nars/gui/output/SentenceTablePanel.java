package nars.gui.output;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import nars.core.NAR;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.gui.NARSwing;
import nars.gui.NWindow;
import nars.gui.output.graph.ProcessingGraphPanel2;
import nars.language.CompoundTerm;
import nars.language.Term;
import nars.util.NARGraph;
import org.jgrapht.graph.DirectedMultigraph;

public class SentenceTablePanel extends TablePanel {

    private final JButton syntaxGraphButton;

    public SentenceTablePanel(NAR nar) {
        super(nar);

        setLayout(new BorderLayout());

        data = newModel();

        table.setModel(data);
        table.setAutoCreateRowSorter(true);
        table.validate();
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                syntaxGraphButton.setEnabled(table.getSelectedRowCount() > 0);
            }
        });
        table.getColumn("Type").setMaxWidth(48);
        table.getColumn("Frequency").setMaxWidth(64);
        table.getColumn("Confidence").setMaxWidth(64);
        table.getColumn("Priority").setMaxWidth(64);
        table.getColumn("Complexity").setMaxWidth(64);
        table.getColumn("Time").setMaxWidth(72);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        {
            syntaxGraphButton = new JButton("Graph");
            syntaxGraphButton.setEnabled(false);
            syntaxGraphButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newSelectedGraphPanel();
                }
            });
            menu.add(syntaxGraphButton);

            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    data = newModel();
                    table.setModel(data);
                }
            });
            menu.add(clearButton);

        }
        add(menu, BorderLayout.SOUTH);
    }

    public DefaultTableModel newModel() {
        DefaultTableModel data = new DefaultTableModel();
        data.addColumn("Time");
        data.addColumn("Sentence");
        data.addColumn("Type");
        data.addColumn("Frequency");
        data.addColumn("Confidence");
        data.addColumn("Complexity");
        data.addColumn("Priority");
        data.addColumn("ParentTask");
        data.fireTableStructureChanged();
        return data;
    }

    public void newSelectedGraphPanel() {
        ProcessingGraphPanel2 pgp = new ProcessingGraphPanel2(getSelectedRows(1)) {

            @Override
            public DirectedMultigraph getGraph() {

                NARGraph.DefaultGraphizer graphizer = new NARGraph.DefaultGraphizer(true, true, true, true, false, false) {

                    protected void addSentence(NARGraph g, Sentence s) {
                        Term t = s.content;
                        addTerm(g, t);
                        //g.addEdge(s, s.content, new NARGraph.SentenceContent());

                        if (t instanceof CompoundTerm) {
                            CompoundTerm ct = ((CompoundTerm) t);
                            Set<Term> contained = ct.getContainedTerms();

                            for (Term x : contained) {
                                addTerm(g, x);
                                if (ct.containsTerm(x)) {
                                    g.addEdge(x, t, new NARGraph.TermContent());
                                }

                                for (Term y : contained) {
                                    addTerm(g, y);

                                    if (x != y) {
                                        if (x.containsTerm(y)) {
                                            g.addEdge(y, x, new NARGraph.TermContent());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onTime(NARGraph g, long time) {
                        super.onTime(g, time);

                        for (Object o : getItems()) {

                            if (o instanceof Task) {
                                g.addVertex(o);
                                addSentence(g, ((Task) o).sentence);
                            } else if (o instanceof Sentence) {
                                g.addVertex(o);
                                addSentence(g, (Sentence) o);
                            }
                        }
                        //add sentences
                    }

                };

                app.updating = true;

                graphizer.setShowSyntax(showSyntax);

                NARGraph g = new NARGraph();
                g.add(nar, newSelectedGraphFilter(), graphizer);
                return g;
            }

            @Override
            public int edgeColor(Object edge) {
                return NARSwing.getColor(edge.toString(), 0.5f, 0.5f).getRGB();
            }

            @Override
            public float edgeWeight(Object edge) {
                return 10;
            }

            @Override
            public int vertexColor(Object vertex) {
                return NARSwing.getColor(vertex.toString(), 0.5f, 0.5f).getRGB();
            }

        };
        NWindow w = new NWindow("", pgp);
        w.setSize(400, 400);
        w.setVisible(true);
    }

    @Override
    public void output(Class channel, Object o) {
        if (o instanceof Task) {
            Task t = (Task) o;
            float priority = t.getPriority();

            Sentence s = t.sentence;

            float freq = -1;
            float conf = -1;
            TruthValue truth = s.truth;
            if (truth != null) {
                freq = truth.getFrequency();
                conf = truth.getConfidence();
            }

            String parentTask = (t.parentTask != null) ? t.parentTask.toStringExternal() : "";

            //TODO use table sort instead of formatting numbers with leading '0's
            data.addRow(new Object[]{
                String.format("%08d", nar.getTime()),
                s,
                s.punctuation,
                freq == -1 ? "" : freq,
                conf == -1 ? "" : conf,
                String.format("%03d", s.content.getComplexity()),
                priority,
                parentTask
            });
        }
    }

}
