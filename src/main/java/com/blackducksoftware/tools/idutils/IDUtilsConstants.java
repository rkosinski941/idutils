/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.idutils;

public class IDUtilsConstants {

    // Properties
    public static final String PROJECT_NAME = "idutils.project.name";
    public static final String PROJECT_PATH = "idutils.project.path";

    public static final String SEARCH_NAMES = "idutils.search.names";

    // Ability for user to skip validation
    // In some cases
    public static final String PERFORM_VALIDATION = "idutils.perform.validation";

    // Internal working props
    public static final String MANIFEST_PREFIX = "manifest-";

    // / Reporting
    public static final String REPORTING_MODE = "idutils.report.mode";
    // If set to true, outputs a list of files that have pending IDs
    public static final String REPORTING_SECTION_PENDING = "idutils.report.pending";
}
