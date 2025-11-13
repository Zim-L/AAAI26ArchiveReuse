package experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import algorithms.SMSEMOA;
import algorithms.SMSEMOARA;
import algorithms.ZAlgorithm;
import core.BinarySet;
import core.BinarySolution;
import core.CrossoverOperator;
import core.DefaultFileOutputContext;
import core.DominanceComparator;
import core.JMetalException;
import core.MutationOperator;
import core.PermutationSolution;
import core.Problem;
import core.SelectionOperator;
import core.SequentialSolutionListEvaluator;
import core.Solution;
import core.SolutionListOutput;
import indicator.WFGHypervolume;
import indicator.Hypervolume;
import problems.Knapsack01;
import problems.MONKLand;
import problems.MOQAP;
import problems.MOTSP;

import operators.*;

public class overhead {

	private static final boolean ARCHIVE = true;
	private static final boolean NOARCHIVE = false;
	
	private static boolean debug = true;

	private static int INDEPENDENT_RUNS;
	private static String experimentBaseDirectory;
	private static String problemInfoDirectory;
	private static String coreNum;
	private static String algID;
	private static String runtimeStr;
	private static int runTime;
	private static int maxEval;
	private static List<Integer> runs;
	private static int populationSize;
	
	public static void main(String[] args)  {
		maxEval = 10000000;
		populationSize = 100;
		if (debug) {
			INDEPENDENT_RUNS = 30;
			experimentBaseDirectory = "D:/Experiment1010/pop100/";
			problemInfoDirectory = "D:/CommonData/";
			coreNum = "6";
			algID = "OnePoint";//"CrMut";
			runtimeStr = "3600";
			runTime = Integer.valueOf(runtimeStr);
		} else {
			if (args.length == 0) {
				throw new JMetalException(
						"Missing argument: experimentBaseDirectory, problemInfoDirectory, numberOfCores, algID, runtime(sec)");
			} else if (args.length == 1) {
				throw new JMetalException("Missing argument: problemInfoDirectory, numberOfCores, algID, runtime(sec)");
			} else if (args.length == 2) {
				throw new JMetalException("Missing argument: numberOfCores, algID, runtime(sec)");
			} else if (args.length == 3) {
				throw new JMetalException("Missing argument: algID, runtime(sec)");
			} else if (args.length == 4) {
				throw new JMetalException("Missing argument: runtime(sec)");
			} else {
				experimentBaseDirectory = args[0];
				problemInfoDirectory = args[1];
				coreNum = args[2];
				algID = args[3];
				runtimeStr = args[4];
				runTime = Integer.valueOf(runtimeStr);
			}
		}

		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", coreNum);

		List<Problem> problems = setupProblems();
		
		List<Integer> runs = new ArrayList<Integer>();
		final int batch = 1;
		final int offset = 5;
		final int start = 9;
		for (int i = start; i < start+batch*offset; i=i+offset)
			runs.add(i);
		runs.stream().forEach( run -> problems.stream().forEach( problem -> runExperiment(problem, run) ) );
		
		System.exit(0);
	}

	public static List<Problem> setupProblems() {
		if (debug)
			System.out.println("Loading problems instances ");
		long t0 = System.currentTimeMillis();
		
		List<Problem> problems = List.of(new
		  MOQAP(500).load("D:/SEMOPLS/Data/QAP-500.txt")//, new 
		);
		if (debug)
			System.out.println("  Done (" + (System.currentTimeMillis() - t0) + "ms)");
		return problems;
	}

	public static Map<String, double[]> getReferencePoints() {
        Map<String, double[]> refPoints = new HashMap<>();
        refPoints.put("KP-100", new double[]{-2515.0000000000000000, -2716.0000000000000000});
        refPoints.put("KP-500", new double[]{-13840.0000000000000000, -13696.0000000000000000});
        refPoints.put("NK-100-10", new double[]{-0.4557920055974312, -0.4594281083134446});
        refPoints.put("NK-500-10", new double[]{-0.4653126621319031, -0.4775838050817161});
        refPoints.put("TSP-100", new double[]{56.6190727660660968, 60.0963967590472024});
        refPoints.put("TSP-500", new double[]{261.3677799776644974, 264.5092881122234303});
        refPoints.put("QAP-100", new double[]{1290375369.1054358482360840, 1293185711.3079488277435303});
        refPoints.put("QAP-500", new double[]{31692372895.8438377380371094, 31647745621.1098556518554688});
        refPoints.put("KP-200", new double[]{-5520.0000000000000000, -5320.0000000000000000});
        refPoints.put("NK-200-10", new double[]{-0.4453927705633157, -0.4558792735132561});
        refPoints.put("TSP-200", new double[]{107.8075785448631194, 110.4330006768590238});
        refPoints.put("QAP-200", new double[]{5370058024.8078613281250000, 5387024686.8716564178466797});
        return refPoints;
    }


	public static void runExperiment(Problem problem, int run) {
		var ref = getReferencePoints();
		long t0, duration;
		
		SMSEMOA alg = createSMSEMOA(problem);
		
		ArrayList<Double> hv = new ArrayList<Double>();
		
		Consumer<ZAlgorithm> monitor = a -> {
			if (a.getT() < 10 || (a.getEvaluations() >= 10 && a.getEvaluations() < 100 && a.getEvaluations()%10==0) 
					|| (a.getEvaluations() >= 100 && a.getEvaluations() < 1000 && a.getEvaluations()%100==0)
					|| (a.getEvaluations() >= 1000 && a.getT() < 10000 && a.getT()%1000==0)
					|| (a.getEvaluations() >= 10000 && a.getEvaluations() < 100000 && a.getEvaluations() % 10000 == 0) 
					|| (a.getEvaluations() >= 100000 && a.getEvaluations() < 1000000 && a.getEvaluations() % 100000 == 0) 
					|| (a.getEvaluations() >= 1000000 &&  a.getEvaluations() % 1000000 == 0)){
				if (a.getName().contentEquals("SMSEMOA")) {
					hv.add(calculateHypervolumeMin(a.getArchive(), ref.get(problem.getName())));
				}
				if (a.getEvaluations()==100000 || a.getEvaluations()==1000000) {
					saveMiddleResult(a, run, a.getEvaluations());
				}
			}
		};
		
		alg.setMonitor(monitor);
		t0 = System.currentTimeMillis();
		alg.run();
		duration = System.currentTimeMillis() - t0;
		saveResult(alg, duration, run);
		saveList(hv, experimentBaseDirectory+problem.getName()+"/"+alg.getName()+"/hv-"+run+".txt");
	}

	
	public static double calculateHypervolumeMin(List<Solution> dataPoints, double[] referencePoint) {
		if (dataPoints.isEmpty()) return 0;
		
        // Sort data points by the first objective (x-coordinate)
        dataPoints.sort(Comparator.comparingDouble(sol -> sol.objectives()[0]));

        // Build sorted list of 2D points
        List<double[]> sortedPoints = new ArrayList<>();
        for (Solution sol : dataPoints) {
            sortedPoints.add(new double[]{sol.objectives()[0], sol.objectives()[1]});
        }

        // Append final vertical segment to reference
        double[] last = sortedPoints.get(sortedPoints.size() - 1);
        sortedPoints.add(new double[]{referencePoint[0], last[1]});

        double hypervolume = 0.0;
        for (int i = 0; i < sortedPoints.size() - 1; i++) {
            double[] p1 = sortedPoints.get(i);
            double[] p2 = sortedPoints.get(i + 1);

            double x1 = p1[0], y1 = p1[1];
            double x2 = p2[0], y2 = p2[1];

            if (y1 <= referencePoint[1] && y2 <= referencePoint[1]) {
                double base = x2 - x1;
                double height = referencePoint[1] - Math.max(y1, y2);
                hypervolume += base * height;
            }
        }

        return hypervolume;
    }
	
	public static void saveMiddleResult(ZAlgorithm alg, int run, int time) {
		var problem = alg.getProblem();
		if (problem.getName().contains("KP") || problem.getName().contains("Knapsack")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveMiddleResult(alg,  (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run, time);
		} else if (problem.getName().contains("NK")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveMiddleResult(alg,  (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run, time);
		} else {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<PermutationSolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((PermutationSolution) ((PermutationSolution) algRes.get(i)).copy());
			saveMiddleResult(alg,  (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run, time);
		}
	}
	
	public synchronized static void saveMiddleResult(ZAlgorithm alg, List<? extends Solution<?>> population,
			String route, int run, int time) {
		Problem problem = alg.getProblem();
		int id = run;

		File dir = new File(route + id + '/');
		if (!dir.exists())
			dir.mkdirs();

		// output variables and objectives
		new SolutionListOutput(population)
				.setVarFileOutputContext(new DefaultFileOutputContext(route + "VAR"+id + "-"+time+".csv", ","))
				.setFunFileOutputContext(new DefaultFileOutputContext(route + "FUN"+id + "-" + time + ".csv", ","))
				.print();

	}


	public static void saveResult(ZAlgorithm alg, long duration, int run) {
		var problem = alg.getProblem();
		if (problem.getName().contains("KP") || problem.getName().contains("Knapsack")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run);
		} else if (problem.getName().contains("NK")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run);
		} else {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<PermutationSolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((PermutationSolution) ((PermutationSolution) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/", run);
		}
	}
	
	private static CrossoverOperator chooseCrossover(Problem problem) {
		return problem.createSolution() instanceof BinarySolution ? new SinglePointCrossover(1.0)
				: (problem.getName().contains("QAP") ? new CycleCrossover(0.9) : new OrderCrossover(1.0));
	}

	private static MutationOperator chooseMutation(Problem problem) {
		return problem.createSolution() instanceof BinarySolution
				? new BitFlipMutation(1.0 / problem.getNumberOfVariables())
				: (problem.getName().contains("QAP") ? new PermutationSwapMutation(0.05) : new InversionMutation(0.05));
	}
	
	public static SMSEMOA createSMSEMOA(Problem problem) {
		int N = populationSize;
		CrossoverOperator crossover = chooseCrossover(problem);
		MutationOperator mutation = chooseMutation(problem);
		SelectionOperator selection = new RandomSelection();

		Hypervolume hv = new WFGHypervolume();
		double offset = 100.0;

		SMSEMOA smsemoa = new SMSEMOA(problem, maxEval, N, offset, crossover, mutation, selection,
				new DominanceComparator(), hv);
		//smsemoa.archive = null;
		smsemoa.setName("SMSEMOA-sortedList");
		return smsemoa;
	}

	public static SMSEMOARA createSMSEMOARA(Problem problem) {
		int N = populationSize;
		CrossoverOperator crossover = chooseCrossover(problem);
		MutationOperator mutation = chooseMutation(problem);
		SelectionOperator selection = new RandomSelection();

		Hypervolume hv = new WFGHypervolume();
		double offset = 100.0;

		SMSEMOARA smsemoa = new SMSEMOARA(problem, maxEval, N, offset, crossover, mutation, selection,
				new DominanceComparator(), hv);
		return smsemoa;
	}

	public synchronized static void saveFinalResult(ZAlgorithm alg, long duration,
			List<? extends Solution<?>> population, String route, int run) {
		Problem problem = alg.getProblem();

		File dir = new File(route);
		if (!dir.exists())
			dir.mkdirs();
		//int id = findSaveID(route);
		int id = run;
		if (debug)
			System.out.println("Saving " + problem.getName() + " " + alg.getName() + " run " + id);

		// output general information

		File file = new File(route + "INFO" + id + ".txt");
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(problem.getName());
			writer.println(alg.getName());
			writer.println("Duration(s):" + duration);
			writer.println("Evaluations:" + alg.getEvaluations());
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// output variables and objectives
		new SolutionListOutput(population)
				.setVarFileOutputContext(new DefaultFileOutputContext(route + "VAR" + id + ".csv", ","))
				.setFunFileOutputContext(new DefaultFileOutputContext(route + "FUN" + id + ".csv", ",")).print();
		
		new SolutionListOutput(alg.getPopulation())
		.setVarFileOutputContext(new DefaultFileOutputContext(route + "VARpop" + id + ".csv", ","))
		.setFunFileOutputContext(new DefaultFileOutputContext(route + "FUNpop" + id + ".csv", ",")).print();
	}

	private static int findSaveID(String route) {
		int id = 0;
		while (true) {
			File dir = new File(route + "INFO" + id + ".txt");
			boolean exists = dir.exists();
			if (exists)
				id++;
			else
				break;
		}
		;
		return id;
	}
	
	public static void saveList(List data, String filename) {
        File file = new File(filename);
        File parentDir = file.getParentFile();

        // Create parent directory if it does not exist
        if (parentDir != null && !parentDir.exists()) {
            boolean dirCreated = parentDir.mkdirs();
            if (!dirCreated) {
                System.err.println("Failed to create directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        // Write data to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (var row : data) {
            	if (row instanceof Double) {
                    writer.write(String.format("%.16f", (Double) row));
                } else {
                    writer.write(row.toString());
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

}
