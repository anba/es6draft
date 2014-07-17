<#--

    Copyright (c) 2012-2014 André Bargull
    Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.

    <https://github.com/anba/es6draft>

-->
<#function formatLicense licenses>
    <#assign licenseString = ""/>
    <#if licenses?size = 0>
        <#assign licenseString = "Unknown License!"/>
    <#elseif licenses?size = 1>
        <#assign licenseString = licenses[0]/>
    <#else>
        <#assign licenseString = licenses[0]/>
        <#list licenses[1..] as license>
            <#assign licenseString = licenseString + " ;" +license/>
        </#list>
    </#if>
    <#return licenseString>
</#function>
<#function formatLicenseFile project>
    <#assign licenseFile = "third_party/LICENSE." + project.name?word_list[0]?upper_case/>
    <#return licenseFile>
</#function>

This software contains the following third party components.

- Mozilla Rhino, licensed under Mozilla Public License, v. 2.0.
  The copyright notice and license is located at:
  third_party/LICENSE.RHINO

- Google V8, licensed under BSD 3-Clause License.
  The copyright notice and license is located at:
  third_party/LICENSE.V8

<#list dependencyMap as dependency>
    <#assign project = dependency.getKey()/>
    <#assign license = dependency.getValue()/>
- ${project.name}, licensed under ${formatLicense(license)}.
  <#if project.licenses?size != 0>
  The copyright notice and license is located at:
  ${formatLicenseFile(project)}
  </#if>
  <#if (project.url!"")?length != 0>
  [${project.url}]
  <#else></#if>

</#list>