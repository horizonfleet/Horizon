import React, { Component } from "react";
import "./App.css";
import "font-awesome/css/font-awesome.min.css";
import Centeredgrid from "./components/Grid/Centeredgrid";
// import datasample from "./data/datasample.json";
import Loader from "./components/Grid/Loader";

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isLoading: true,
      datasample: [],
      delayFilter: [0, 90],
      isAlertsOnly: 0,
    };
    this.setDelayFilter = this.setDelayFilter.bind(this);
    this.loadData = this.loadData.bind(this);
    this.filterDelay = this.filterDelay.bind(this);
    this.handleAlertSwitch = this.handleAlertSwitch.bind(this);
    this.handleClear = this.handleClear.bind(this);
  }

  // When component mounts load data, after that the load function gets triggered every 60 seconds
  componentDidMount() {
    this.loadData();
    setInterval(this.loadData, 120000);
  }

  //Get Data from API
  async loadData() {
    try {
      const res = await fetch("/data");
      const blocks = await res.json();

      this.setState(
        {
          datasample: [...blocks],
          // isLoading: false,
        },
        () => {
          this.setState({ isLoading: false });
        }
      );
    } catch (e) {
      console.log(e);
    }
  }

  //When the Delay Slider has been adjustet, the state will be updated with the slide values.
  //From there, the filtered data will be reached through to the other components.
  setDelayFilter(event, delaySpan) {
    this.setState({ delayFilter: delaySpan });
  }

  //Function for checking, if a datapoint matches the filter criteria of the state.
  filterDelay(datapoint) {
    //The current data format is in seconds. For usability reasons, the value is converted to minutes.
    let delayInMinutes = (datapoint.delay / 60).toFixed(0);

    if (delayInMinutes < 0 && this.state.delayFilter[0] === 0) {
      return true;
    } else if (delayInMinutes > 90 && this.state.delayFilter[0] === 90) {
      return true;
    } else if (
      delayInMinutes >= this.state.delayFilter[0] &&
      delayInMinutes <= this.state.delayFilter[1]
    ) {
      return true;
    } else {
      return false;
    }
  }

  //Function for handling an alert switch event
  handleAlertSwitch(event) {
    console.log("switch switch", [event.target.name], event.target.checked);
    this.setState(
      { isAlertsOnly: event.target.checked ? 0 : -1 },
      console.log(this.state.isAlertsOnly)
    );
  }

  //Function for transforming the consumption of a datapoint into a harmonized value
  getTruckConsumptionStatus(datapoint) {
    let consumption_ratio =
      datapoint.normal_consumption / datapoint.truck_consumption;
    let tolerance = 0.05;
    if (1 < consumption_ratio + tolerance && 1 <= consumption_ratio) {
      return 0;
    } else if (1 <= consumption_ratio + tolerance && 1 > consumption_ratio) {
      return 1;
    } else {
      return 2;
    }
  }

  //Function for counting the conspicuous warnings of the datapoint
  conspicuousCounter(datapoint) {
    let conspicuousCount = 0;
    if (datapoint.driver_acceleration === 1) {
      conspicuousCount++;
    }
    if (datapoint.driver_speed === 1) {
      conspicuousCount++;
    }
    if (datapoint.driver_brake === 1) {
      conspicuousCount++;
    }
    if (this.getTruckConsumptionStatus(datapoint) === 1) {
      conspicuousCount++;
    }
    if (datapoint.engine_efficiency === 1) {
      conspicuousCount++;
    }
    if (datapoint.tires_efficiency === 1) {
      conspicuousCount++;
    }
    if (datapoint.truck_condition === 1) {
      conspicuousCount++;
    }
    return conspicuousCount;
  }

  //Function for counting the bad warnings of the datapoint
  badCounter(datapoint) {
    let badCount = 0;
    if (datapoint.driver_acceleration === 2) {
      badCount++;
    }
    if (datapoint.driver_speed === 2) {
      badCount++;
    }
    if (datapoint.driver_brake === 2) {
      badCount++;
    }
    if (this.getTruckConsumptionStatus(datapoint) === 2) {
      badCount++;
    }
    if (datapoint.engine_efficiency === 2) {
      badCount++;
    }
    if (datapoint.tires_efficiency === 2) {
      badCount++;
    }
    if (datapoint.truck_condition === 2) {
      badCount++;
    }
    return badCount;
  }

  // Clear All Filters
  handleClear(event) {
    this.setState({ delayFilter: [0, 90] }, () => {
      console.log(this.state);
    });
  }

  render() {
    //Before the component is rendered, the data is filtered using the filter settings.
    let filteredData = this.state.datasample.filter(
      (datapoint) =>
        this.filterDelay(datapoint) &&
        (this.conspicuousCounter(datapoint) > this.state.isAlertsOnly ||
          this.badCounter(datapoint) > this.state.isAlertsOnly)
    );

    return (
      <React.Fragment>
        {/* <Grid container spacing={3}>
          <Grid item xs={12}>
            <Paper>
              {this.state.isLoading ? (
                "Loading..."
              ) : (
                <Slidertest
                  delayFilter={this.state.delayFilter}
                  setDelayFilter={this.setDelayFilter}
                ></Slidertest>
              )}
            </Paper>
          </Grid>
          {filteredData.map((datapoint) => (
            <Grid item xs={12}>
              <Paper>
                <Slidertestresult datapoint={datapoint} />
              </Paper>
            </Grid>
          ))}
        </Grid> */}
        {/* {this.state.isLoading ? (
          <div style={{ width: "100%", marginTop: "200px" }}>Loading...</div>
        ) : ( */}

        <Loader isLoading={this.state.isLoading}></Loader>
        <Centeredgrid
          state={this.state}
          handleDelayChange={this.setDelayFilter}
          handleClear={this.handleClear}
          datasample={filteredData}
          handleAlertSwitch={this.handleAlertSwitch}
        />
      </React.Fragment>
    );
  }
}

export default App;
