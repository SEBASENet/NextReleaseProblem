package neo.requirements.sat.algorithms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import neo.requirements.sat.cplex.ILPAdaptor;
import neo.requirements.sat.cplex.Modelo;
import neo.requirements.sat.util.EfficientSolutionWithTimeStamp;
import neo.requirements.sat.util.SingleThreadCPUTimer;

public class ChicanoEpsilonConstraint implements ILPBasedBiobjectiveSolver {

	private static final int VALUE_OBJECTIVE = 1;
	private static final int EFFORT_OBJECTIVE = 0;

	@Override
	public List<EfficientSolutionWithTimeStamp> computeParetoFront(ILPAdaptor adaptor) {
		SingleThreadCPUTimer timer = new SingleThreadCPUTimer();
		timer.startTimer();
		try {
			List<EfficientSolutionWithTimeStamp> paretoFront = new ArrayList<>();

			Modelo modelo = adaptor.ilpModelForConstraints();
			modelo.cplex.addMinimize(adaptor.getObjective(VALUE_OBJECTIVE));

			while (solveIlpInstance(modelo)) {
				double value = (int)modelo.cplex.getObjValue();

				modelo = adaptor.ilpModelForConstraints();
				modelo.cplex.addLe(adaptor.getObjective(VALUE_OBJECTIVE), value);
				modelo.cplex.addMinimize(adaptor.getObjective(EFFORT_OBJECTIVE));

				solveIlpInstance(modelo);

				double effort = (int)modelo.cplex.getObjValue();

				paretoFront.add(new EfficientSolutionWithTimeStamp(new double [] {-effort, value},  timer.elapsedTimeInMilliseconds()));

				modelo = adaptor.ilpModelForConstraints();
				modelo.cplex.addLe(adaptor.getObjective(EFFORT_OBJECTIVE), effort-1);
				modelo.cplex.addMinimize(adaptor.getObjective(VALUE_OBJECTIVE));
			}
			return paretoFront;
		} catch (IloException e) {
			throw new RuntimeException (e);
		}
	}
	
	public boolean solveIlpInstance(Modelo modelo) throws IloException {	
			modelo.cplex.setOut(null);
			modelo.cplex.setParam(IloCplex.DoubleParam.EpInt, 1E-9);
			modelo.cplex.setParam(IloCplex.DoubleParam.EpGap, 1E-9);
			modelo.cplex.setParam(IloCplex.DoubleParam.EpOpt, 1E-9);
			return modelo.cplex.solve();
	}

}