import React, { Component } from "react";
import "../../node_modules/react-vis/dist/style.css";
import {
  XYPlot,
  LineSeries,
  VerticalBarSeries,
  VerticalGridLines,
  HorizontalGridLines,
  HorizontalBarSeries,
  XAxis,
  YAxis,
  Hint,
} from "react-vis";

class Delayalarm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: props.data,
    };
  }

  render() {
    let zero = this.state.data.filter(
      (datapoint) => parseInt(datapoint.truck_delay) === 0
    );
    let oneFifteen = this.state.data.filter(
      (datapoint) =>
        0 < parseInt(datapoint.truck_delay) &&
        parseInt(datapoint.truck_delay) < 15
    );
    let sixteenThirty = this.state.data.filter(
      (datapoint) =>
        15 < parseInt(datapoint.truck_delay) &&
        parseInt(datapoint.truck_delay) < 31
    );
    let thirtyoneFourtyfive = this.state.data.filter(
      (datapoint) =>
        30 < parseInt(datapoint.truck_delay) &&
        parseInt(datapoint.truck_delay) < 46
    );
    let fourtysixSixty = this.state.data.filter(
      (datapoint) =>
        45 < parseInt(datapoint.truck_delay) &&
        parseInt(datapoint.truck_delay) < 61
    );
    let biggerSixty = this.state.data.filter(
      (datapoint) =>
        60 < parseInt(datapoint.truck_delay) && parseInt(datapoint.truck_delay)
    );

    console.log("length", oneFifteen.length);

    const graphData = [
      { x: "0", y: zero.length },
      { x: "1-15", y: oneFifteen.length },
      { x: "16-30", y: sixteenThirty },
      { x: "31-45", y: thirtyoneFourtyfive.length },
      { x: "46-60", y: fourtysixSixty.length },
      { x: "60<", y: biggerSixty.length },
    ];
    return (
      <div className="Delayalarm">
        <XYPlot height={300} width={300} xType="ordinal">
          <XAxis />
          <YAxis />
          <VerticalBarSeries data={graphData} color="#1A90FF" barWidth="0.3" />
        </XYPlot>
      </div>
    );
  }
}

export default Delayalarm;
