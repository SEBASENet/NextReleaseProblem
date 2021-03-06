package neo.requirements.cplex.algorithms;

import java.util.Properties;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import neo.requirements.cplex.Modelo;
import neo.requirements.util.EfficientSolution;

public class AnytimeAugmecon extends AbstractAnytime {

	protected Double lambdaValue = null;

	@Override
	public String getName() {
		return "Anytime Augmecon";
	}

	@Override
	public String getCommandLineName() {
		return "anytime-augmecon";
	}

	protected EfficientSolution getNonDominatedSolutionInRectangle(PairOfEfficientSolutions pair)
			throws IloException {

		double epsilon = getEpsilonForPair(pair);

		double lambda;
		if (lambdaValue != null) {
			lambda = lambdaValue;
		} else {
			lambda=adaptor.minimumDifferenceBetweenEfficientSolutions(firstObjective())/
					(epsilon-adaptor.idealLowerBound(secondObjective()));
			reportLambda(lambda);
		}

		Modelo modelo = adaptor.ilpModelForConstraints();
		IloLinearNumExpr objective = adaptor.getObjective(firstObjective());
		IloNumVar ell = modelo.cplex.numVar(0, Double.POSITIVE_INFINITY);
		objective.addTerm(-lambda, ell);
		modelo.cplex.addMinimize(objective);
		IloLinearNumExpr secondObjective = adaptor.getObjective(secondObjective());
		secondObjective.addTerm(1.0, ell);
		modelo.cplex.addEq(secondObjective, epsilon);

		solveIlpInstance(modelo);
		double firstObjValue = (int)Math.round(evaluateLinearExpression(modelo, adaptor.getObjective(firstObjective())));
		double secondObjValue = (int)Math.round(evaluateLinearExpression(modelo, adaptor.getObjective(secondObjective())));

		return buildEfficientSolution(timer, firstObjValue, secondObjValue);
	}

	protected void addNewPairIfRequired(PairOfEfficientSolutions pair) {
		PairOfEfficientSolutions newPair = pair.clone();
		newPair.lower = buildEfficientSolution(timer, pair.lower.getObjectiveValue(firstObjective()), getEpsilonForPair(pair));
		queue.add(newPair);
	}
	
	@Override
	public void setConfiguration(Properties configuration) {
		super.setConfiguration(configuration);
		configureLambda(configuration);
	}

	protected void configureLambda(Properties configuration) {
		String lambdaProperty= configuration.getProperty("lambda");
		if (lambdaProperty!=null) {
			if ("adaptive".equals(lambdaProperty)) {
				lambdaValue=null;
			} else {
				lambdaValue = Double.parseDouble(lambdaProperty);
			}
		}
	}

}
