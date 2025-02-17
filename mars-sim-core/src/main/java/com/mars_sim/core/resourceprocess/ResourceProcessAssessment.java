package com.mars_sim.core.resourceprocess;

import java.io.Serializable;

public record ResourceProcessAssessment(double inputScore, double outputScore, double overallScore,
                        boolean inputsAvailable)
        implements Serializable {

}
