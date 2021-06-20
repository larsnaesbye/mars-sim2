/**
 * Mars Simulation Project
 * JAXAMissionControl.java
 * @version 3.1.2 2020-09-02
 * @author Manny Kung
 */

package org.mars_sim.msp.core.reportingAuthority;

import java.io.Serializable;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.reportingAuthority.objectives.DevelopingSpaceApplications;

/*
 * This class represents the Mission Control of the Japan Aerospace Exploration Agency
 */
public class JAXAMissionControl extends ReportingAuthority
implements Serializable {
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//private final String name = "JAXA";

	private final ReportingAuthorityType org = ReportingAuthorityType.JAXA;

	private final String toolTipStr = "Japan Aerospace Exploration Agency";

	private Unit unit;

	public String getToolTipStr() {
		return toolTipStr;
	}

	//public String getName() {
	//	return name;
	//}

	private JAXAMissionControl(Unit unit) {
		this.unit = unit;
		missionAgenda = new DevelopingSpaceApplications(unit);
	}

	public static JAXAMissionControl createMissionControl(Unit unit) {
		return new JAXAMissionControl(unit);
	}

	@Override
	public ReportingAuthorityType getOrg() {
		return org;
	}

}
