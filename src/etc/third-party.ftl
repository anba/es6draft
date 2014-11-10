<#--

    Copyright (c) 2012-2014 André Bargull
    Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.

    <https://github.com/anba/es6draft>

-->
<#function formatLicense licenses>
    <#local licenseString = ""/>
    <#if licenses?size = 0>
        <#local licenseString = "Unknown License!"/>
    <#elseif licenses?size = 1>
        <#local licenseString = licenses[0]/>
    <#else>
        <#local licenseString = licenses[0]/>
        <#list licenses[1..] as license>
            <#local licenseString = licenseString + " ;" + license/>
        </#list>
    </#if>
    <#return licenseString>
</#function>
<#function formatLicenseFile project>
    <#local licenseFile = "third_party/LICENSE." + project.name?word_list[0]?upper_case/>
    <#return licenseFile>
</#function>
<#function readBufferedReader reader padString>
    <#local lineContent = (reader.readLine())!""/>
    <#if lineContent?length != 0>
        <#local nextLine = readBufferedReader(reader, padString)/>
        <#local lineString = padString + lineContent + "\n" + nextLine/>
    <#else>
        <#local lineString = ""/>
    </#if>
    <#return lineString>
</#function>
<#macro includeNoticeFile project>
    <#local noticeFile = "./src/licenses/NOTICE." + project.name?word_list[0]?upper_case/>
    <#local objectConstructor = "freemarker.template.utility.ObjectConstructor"?new()/>
    <#local file = objectConstructor("java.io.File", noticeFile)/>
    <#if file.isFile()>
        <#local fileReader = objectConstructor("java.io.FileReader", file)/>
        <#local bufferedReader = objectConstructor("java.io.BufferedReader", fileReader)/>
  ${project.name} requires the following note to be included in this NOTICE file:
${readBufferedReader(bufferedReader, "    ")?chop_linebreak}
        <#local closeReader = bufferedReader.close()/>
    </#if>
</#macro>
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
  <@includeNoticeFile project=project/>

</#list>