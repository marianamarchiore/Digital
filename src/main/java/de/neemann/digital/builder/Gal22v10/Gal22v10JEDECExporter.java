package de.neemann.digital.builder.Gal22v10;

import de.neemann.digital.analyse.expression.Expression;
import de.neemann.digital.analyse.expression.Variable;
import de.neemann.digital.builder.*;
import de.neemann.digital.builder.jedec.FuseMap;
import de.neemann.digital.builder.jedec.FuseMapFiller;
import de.neemann.digital.builder.jedec.FuseMapFillerException;
import de.neemann.digital.builder.jedec.JedecWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to create a JEDEC file for the Lattice GAL16V8
 *
 * @author hneemann
 */
public class Gal22v10JEDECExporter implements ExpressionExporter<Gal22v10JEDECExporter> {
    private static final int[] PRODUCTS_BY_OLMC = new int[]{8, 10, 12, 14, 16, 16, 14, 12, 10, 8};
    private static final int[] OE_FUSE_NUM_BY_OLMC = new int[]{44, 440, 924, 1496, 2156, 2904, 3652, 4312, 4884, 5368};
    private static final int S0 = 5808;
    private static final int S1 = 5809;
    private final FuseMap map;
    private final FuseMapFiller filler;
    private final BuilderCollector builder;
    private final PinMap pinMap;

    /**
     * Creates new instance
     */
    public Gal22v10JEDECExporter() {
        map = new FuseMap(5892);
        filler = new FuseMapFiller(map, 22);

        builder = new BuilderCollector() {
            @Override
            public BuilderCollector addCombinatorial(String name, Expression expression) throws BuilderException {
                if (pinMap.isSimpleAlias(name, expression))
                    return this;
                else
                    return super.addCombinatorial(name, expression);
            }
        };
        pinMap = new PinMap()
                .setAvailInputs(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
                .setAvailOutputs(14, 15, 16, 17, 18, 19, 20, 21, 22, 23);

    }

    @Override
    public BuilderCollector getBuilder() {
        return builder;
    }

    @Override
    public PinMap getPinMapping() {
        return pinMap;
    }

    @Override
    public void writeTo(OutputStream out) throws FuseMapFillerException, IOException, PinMapException {
        for (String in : builder.getInputs()) {
            int i = pinMap.getInputFor(in) - 1;
            filler.addVariable(i * 2, new Variable(in));
        }
        for (String o : builder.getOutputs()) {
            int i = 23 - pinMap.getOutputFor(o);
            filler.addVariableReverse(i * 2 + 1, new Variable(o));
        }

        for (String o : builder.getOutputs()) {
            int olmc = 23 - pinMap.getOutputFor(o);
            int offs = OE_FUSE_NUM_BY_OLMC[olmc];
            for (int j = 0; j < 44; j++) map.setFuse(offs + j); // turn on OE
            map.setFuse(S0 + olmc * 2);                         // set olmc to active high
            if (builder.getCombinatorial().containsKey(o)) {
                map.setFuse(S1 + olmc * 2);
                filler.fillExpression(offs + 44, builder.getCombinatorial().get(o), PRODUCTS_BY_OLMC[olmc]);
            } else if (builder.getRegistered().containsKey(o)) {
                filler.fillExpression(offs + 44, builder.getRegistered().get(o), PRODUCTS_BY_OLMC[olmc]);
            } else
                throw new FuseMapFillerException("variable " + o + " not found!");
        }

        new JedecWriter(out).println("Digital GAL22v10 assembler*").write(map).close();
    }

}