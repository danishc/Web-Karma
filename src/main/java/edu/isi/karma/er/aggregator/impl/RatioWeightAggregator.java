package edu.isi.karma.er.aggregator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import edu.isi.karma.er.aggregator.Aggregator;
import edu.isi.karma.er.helper.RatioFileUtil;
import edu.isi.karma.er.helper.entity.MultiScore;
import edu.isi.karma.er.helper.entity.Score;
import edu.isi.karma.er.helper.entity.ScoreType;
import edu.isi.karma.er.matcher.Matcher;
import edu.isi.karma.er.matcher.impl.NumberMatcher;
import edu.isi.karma.er.matcher.impl.NumberSetMatcher;
import edu.isi.karma.er.matcher.impl.StringMatcher;
import edu.isi.karma.er.matcher.impl.StringSetMatcher;

public class RatioWeightAggregator implements Aggregator {

	private Map<String, Map<String, Double>> ratioMapList = null;
	
	public RatioWeightAggregator(Map<String, Map<String, Double>> ratioMapList) {
		setRatioMapList(ratioMapList);
	}
	
	public MultiScore match(JSONArray confArr, Resource res1, Resource res2) {
		MultiScore ms = new MultiScore();
		ms.setSrcSubj(res1);
		ms.setDstSubj(res2);
		List<Score> sList = new ArrayList<Score>();
		
		// for each property to be compared in configuration array, load its configurations and initialize the detailed comparator to be invoked.
		for (int i = 0; i < confArr.length(); i++) {	
			try {
				JSONObject config = confArr.getJSONObject(i);
				Matcher m = null;
				
				if ("string".equalsIgnoreCase(config.optString("type"))) {		// if the property is string-type
					JSONObject paramConfig = config.getJSONObject("comparator");
					
					if (true == config.getBoolean("is_set")) {					// if this property possibly has several values.
						m = new StringSetMatcher(paramConfig);
						
					} else {													// else has only single value
						m = new StringMatcher(paramConfig);
					}
					
				} else if ("number".equalsIgnoreCase(config.optString("type"))) { // if the property is numberic-type
					JSONObject paramConfig = config.getJSONObject("comparator");
					
					if (true == config.getBoolean("is_set")) {
						m = new NumberSetMatcher(paramConfig);
					} else {
						m = new NumberMatcher(paramConfig);
					}
				}
				
				String predicate = config.getString("property");				// create the property object to be compared
				Property p = ResourceFactory.createProperty(predicate);
				
				Score s = m.match(p, res1, res2);		
				sList.add(s);
				
				
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		ms.setScoreList(sList);				// add the detailed compare result of each property into a score list.
		
		ms.setFinalScore(calcRatio(sList));	// aggregate score of properties
		return ms;
	}

	/**
	 * aggregator the final score from details score objects in list.
	 * @param sList  the list of score objects for compared properties.
	 * @return the final gross score of matching.
	 */
	private double calcRatio(List<Score> sList) {
		double finalScore = 0;
		if (sList == null || sList.size() <= 0)
			return finalScore;
		
		double sim, ratio, weight, totalSim = 0, totalWeight = 0;
		for (Score s : sList) {
			if (s.getScoreType() == ScoreType.NORMAL) {
				sim = s.getSimilarity();
				ratio = getRatio(s.getPredicate().getURI()
						, s.getSrcObj().getObject().toString()
						, s.getDstObj().getObject().toString());
				weight = calcWeight(sim, ratio);
				totalSim += sim * weight;
				totalWeight += weight;
			} else  {
				ratio = getRatio(s.getPredicate().getURI(), null, null);
				s.setSimilarity(ratio);
				sim = ratio;
				weight = calcWeight(sim, ratio);
				totalSim += sim * weight;
				totalWeight += weight;
			}
		}
		finalScore = totalSim / totalWeight;
		return finalScore;
	}


	private double calcWeight(double sim, double ratio) {
		if (ratio > 0) {
			return Math.sqrt(1/ratio);
		}
		return 0;
	}

	/**
	 * Load ratio of the given property from Ratio Map
	 * @param uri	the full uri of given property
	 * @param srcValue  the value from source resource to query for ratio
	 * @param dstValue  the value from target resource to query for ratio
	 * @return the ratio value, especially return average ratio (ratio size of total count) when source and target resource are null.
	 */
	private double getRatio(String uri, String srcValue, String dstValue) {
		/*
		Map<String, Double> map = ratioMapList.get(uri);
		if (map == null) 
			throw new IllegalArgumentException("Ratio files load error.");
		*/
		RatioFileUtil util = new RatioFileUtil();
		Map<String, Double> map = util.getDefaultRatio();
		return map.get(uri);
		
	}
	
	public void setRatioMapList(Map<String, Map<String, Double>> ratioMapList) {
		this.ratioMapList = ratioMapList;
	}
	
	public Map<String, Map<String, Double>> getRatioMapList() {
		return this.ratioMapList;
	}

}
