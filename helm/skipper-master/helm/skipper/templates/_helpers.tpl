{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "skipper.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "skipper.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "skipper.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "skipper.labels" -}}
helm.sh/chart: {{ include "skipper.chart" . }}
{{ include "skipper.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}


{{/* Selector labels */}}
{{- define "skipper.selectorLabels" -}}
{{- range $key, $value := .Values.selectorLabels -}}
{{ $key }}: {{ $value | quote }}
{{ end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "skipper.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "skipper.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/* ENV vars */}}
{{- define "skipper.envVars" -}}
{{- range $k, $v := .Values.envVars }}
- name: {{ index $v "name" }}
  value: {{ tpl ( index $v "value" ) $ | quote }}
{{- end }}
{{- end }}