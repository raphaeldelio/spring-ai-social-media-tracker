package com.redis.socialmediatracker.agent.collectoragent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FetchedDataResponse {
    private Map<String, Object> searchParameters;
    private List<FetchedPost> fetchedData;
    private String dataQualityNotes;
    private String nextSteps;

    public FetchedDataResponse(Map<String, Object> searchParameters,
                               List<FetchedPost> fetchedData,
                               String dataQualityNotes,
                               String nextSteps) {
        this.searchParameters = searchParameters;
        this.fetchedData = fetchedData;
        this.dataQualityNotes = dataQualityNotes;
        this.nextSteps = nextSteps;
    }

    public Map<String, Object> getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(Map<String, Object> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public List<FetchedPost> getFetchedData() { return fetchedData; }
    public void setFetchedData(List<FetchedPost> fetchedData) { this.fetchedData = fetchedData; }

    public String getDataQualityNotes() {
        return dataQualityNotes;
    }

    public void setDataQualityNotes(String dataQualityNotes) {
        this.dataQualityNotes = dataQualityNotes;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FetchedDataResponse that = (FetchedDataResponse) o;
        return Objects.equals(searchParameters, that.searchParameters) && Objects.equals(fetchedData, that.fetchedData) && Objects.equals(dataQualityNotes, that.dataQualityNotes) && Objects.equals(nextSteps, that.nextSteps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchParameters, fetchedData, dataQualityNotes, nextSteps);
    }

    @Override
    public String toString() {
        return "FetchedDataResponse{" +
                "searchParameters=" + searchParameters +
                ", fetchedData=" + fetchedData +
                ", dataQualityNotes='" + dataQualityNotes + '\'' +
                ", nextSteps='" + nextSteps + '\'' +
                '}';
    }
}