package process;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.Session;

import entity.IrCurveHis;
import entity.TransitionMatrix;
import esg.CoveredBond;
import esg.Matrix;
import esg.StringMatrix;
import esg.StringVector;
import esg.Vector;
import util.ECreditGrade;
import util.HibernateUtil;


public class Main {
	
	private static Session session;

	public static void main(String[] args) {
		session = HibernateUtil.getSessionFactory().openSession();
		job51();
	}
	
	public static void job20() {
		
		session.beginTransaction();
		List<IrCurveHis> curveHis = session.createQuery("FROM IrCurveHis").getResultList();
		session.getTransaction().commit();
		session.close();
				
		int n = curveHis.size();
		String[][] data = new String[n][4];
		for(int i=0; i<n; i++) {
			data[i][0] = curveHis.get(i).getBaseDate();
			data[i][1] = curveHis.get(i).getIrCurveId();
			data[i][2] = curveHis.get(i).getMatCd();
			data[i][3] = String.valueOf(curveHis.get(i).getIntRate());
		}
		StringMatrix M = new StringMatrix(data);
		M.sortRowVector(new int[] {0, 1});
		Matrix ktbRates = M.filterRowVector(1, "A100").pivotTableSum(new int[] {0}, new int[] {2}, 3);
		Matrix kdbRates = M.filterRowVector(1, "E110").pivotTableSum(new int[] {0}, new int[] {2}, 3);
		double[] maturity = ktbRates.getColumnNames()
			.stream()
			.mapToDouble(x -> Double.parseDouble(x.get(0).replaceAll("M",  ""))/12.)
			.toArray();
		CoveredBond cb = new CoveredBond(maturity, kdbRates.getData(), ktbRates.getData(), ktbRates.getRowVector(ktbRates.getRowDimension()-1).getData());
		cb.calcBeta();
		double[] v = Vector.createRangeVector(241).scalarMultiply(1./12.).getData();
		Vector w = new Vector(cb.getLiquidPremium(v));
		w.print();
		
	}
	
	public static void job51() {
		
		session.beginTransaction();
		List<TransitionMatrix> tm = session.createQuery("FROM TransitionMatrix").getResultList();
		session.getTransaction().commit();
		session.close();
		
		List<TransitionMatrix> tm2017 = tm.stream().filter(r -> r.getBaseYyyy().equals("2017")).collect(toList());
		
		int n = tm2017.size();
		String[][] data = new String[n][3];
		for(int i=0; i<n; i++) {
			data[i][0] = tm2017.get(i).getFromCrdGrdCd();
			data[i][1] = tm2017.get(i).getToCrdGrdCd();
			data[i][2] = String.valueOf(tm2017.get(i).getProbRate());
		}
		StringMatrix M = new StringMatrix(data);
		Matrix transMat = M.pivotTableAvg(new int[] {0}, new int[] {1}, 2);
		
		int m = transMat.getRowDimension();
		transMat.print();
		transMat.addRowVector(m, Vector.createUnitVector(m, m+1));
		
		
		int k = 100;
		Matrix transMatX = transMat.power(k);
		Vector cumPd = transMatX.getColumnVector(m);
//		transMatX.print();
		
		transMat.deleteRowVector(m);
		Matrix transMat2 = transMat.copy();
		Vector pd = transMat2.getColumnVector(m); 
		transMat2.deleteColumnVector(m);
		Vector fwdPd = transMat2.power(k-1).operate(pd);
		cumPd.print();
		fwdPd.print();
		
	}

}
