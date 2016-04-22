package neo.requirements.cplex.algorithms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import neo.requirements.cplex.ILPAdaptor;
import neo.requirements.cplex.ILPBasedBiobjectiveSolver;
import neo.requirements.cplex.ILPSolverListener;
import neo.requirements.cplex.Modelo;
import neo.requirements.util.EfficientSolution;
import neo.requirements.util.EfficientSolutionWithTimeStamp;
import neo.requirements.util.SingleThreadCPUTimer;

public class ChicanoEpsilonConstraint extends AbstractILPBasedBIobjectiveSolver {
	
	private static final int STEP = 1;

	@Override
	public List<EfficientSolution> computeParetoFront(ILPAdaptor adaptor) {
		SingleThreadCPUTimer timer = new SingleThreadCPUTimer();
		timer.startTimer();
		try {
			createParetoFront();

			Modelo modelo = adaptor.ilpModelForConstraints();
			modelo.cplex.addMinimize(adaptor.getObjective(firstObjective()));

			while (solveIlpInstance(modelo)) {
				double firstObjValue = (int)Math.round(modelo.cplex.getObjValue());

				modelo = adaptor.ilpModelForConstraints();
				modelo.cplex.addLe(adaptor.getObjective(firstObjective()), firstObjValue);
				modelo.cplex.addMinimize(adaptor.getObjective(secondObjective()));

				solveIlpInstance(modelo);

				double secondObjValue = (int)Math.round(modelo.cplex.getObjValue());

				EfficientSolution efficientSolution = new EfficientSolutionWithTimeStamp(new double [] {firstObjValue, secondObjValue},  timer.elapsedTimeInMilliseconds());
				reportEfficientSolution(efficientSolution);

				modelo = adaptor.ilpModelForConstraints();
				modelo.cplex.addLe(adaptor.getObjective(secondObjective()), secondObjValue-STEP);
				modelo.cplex.addMinimize(adaptor.getObjective(firstObjective()));
			}
			return paretoFront;
		} catch (IloException e) {
			throw new RuntimeException (e);
		}
	}

	protected int secondObjective() {
		return 1;
	}

	protected int firstObjective() {
		return 0;
	}
	
	@Override
	public String getName() {
		return "Chicano epsilon constraint";
	}

	@Override
	public String getCommandLineName() {
		return "chicano";
	}

}
