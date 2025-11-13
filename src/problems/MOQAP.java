package problems;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;

import core.AbstractIntegerPermutationProblem;
import core.JMetalRandom;
import core.PermutationSolution;


public class MOQAP extends AbstractIntegerPermutationProblem {

	int n;
	int M = 2;
	double weightLimit;
	double opt1, opt2;
	double[] x;
	double[] y;
	double[][] distance;

	private double[][][] flows; // flow matrix

	public MOQAP() {
		initProblem(50);
	}

	public MOQAP(int n) {
		initProblem(n);
	}

	public void initProblem(int n) {
		this.n = n;
		setNumberOfVariables(n);
		setNumberOfObjectives(M);
		setName("QAP-" + n);

		JMetalRandom random = JMetalRandom.getInstance();
		x = new double[n];
		y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = random.nextDouble(0, 5000);
			y[i] = random.nextDouble(0, 5000);
		}
		// calculate distance matrix
		distance = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i; j < n; j++) {
				distance[i][j] = Math.sqrt((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j]));
				distance[j][i] = distance[i][j];
			}
		}

		// create flow matrix
		flows = new double[M][n][n];
		for (int m = 0; m < M; m++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					flows[m][i][j] = random.nextDouble(0, 100);
				}
			}
		}
	}

	@Override
	public int getLength() {
		return n;
	}

	public void save(String path) {
		File file = new File(path);
		if (file.exists()) {
			System.out.println("Save failed, file exists");
			return;
		}
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(n);
			writer.println(M);
			// save distance
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					writer.print(distance[i][j] + " ");
				}
				writer.println();
			}

			// save matrix
			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						writer.print(flows[m][i][j] + " ");
					}
					writer.println();
				}
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public MOQAP load(String path) {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(path);
			DataInputStream reader = new DataInputStream(inputStream);
			this.n = Integer.valueOf(reader.readLine());
			this.M = Integer.valueOf(reader.readLine());
			setNumberOfVariables(n);
			setNumberOfObjectives(M);

			for (int i = 0; i < n; i++) {
				String[] str = reader.readLine().split(" ");
				for (int j = 0; j < n; j++) {
					distance[i][j] = Double.valueOf(str[j]);
				}
			}

			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					String[] str = reader.readLine().split(" ");
					for (int j = 0; j < n; j++) {
						flows[m][i][j] = Double.valueOf(str[j]);
					}
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}

	@Override
	public PermutationSolution<Integer> evaluate(PermutationSolution<Integer> solution) {
	    final int n       = solution.variables().size();
	    final int M       = flows.length;
	    final double[][]  distance = this.distance;
	    final double[][][] flows    = this.flows;
	    final int[] perm  = solution.variables()
	                                  .stream()
	                                  .mapToInt(Integer::intValue)
	                                  .toArray();
	    final double[] objectives = solution.objectives();

	    Arrays.fill(objectives, 0.0);

	    for (int k = 0; k < M; k++) {
	        double cost = 0.0;
	        final double[][] flowK = flows[k];
	        for (int i = 0; i < n; i++) {
	            final int pi = perm[i];
	            for (int j = 0; j < n; j++) {
	                cost += distance[i][j] * flowK[pi][perm[j]];
	            }
	        }
	        objectives[k] = cost;
	    }

	    return solution;
	}

}
