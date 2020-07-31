import React, { Component } from "react";
import Clock from "react-live-clock";
import Detailrow from "./Detailrow";
import Filtersort from "./Filtersort";
import { FaChartBar, FaMapMarked, FaCircle, FaHistory } from "react-icons/fa";

class Trucklist extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: "data_id",
      sortDirection: "asc",
      data: props.data,
    };
    this.handleChange = this.handleChange.bind(this);
    this.setSortDirection = this.setSortDirection.bind(this);
  }

  componentDidMount() {
    this.setState({
      data: this.state.data.sort((a, b) =>
        this.state.sortDirection === "asc"
          ? a[this.state.value] - b[this.state.value]
          : b[this.state.value] - a[this.state.value]
      ),
    });
  }

  handleChange(arg) {
    this.setState({
      value: arg,
      data: this.state.data.sort((a, b) =>
        this.state.sortDirection === "asc" ? a[arg] - b[arg] : b[arg] - a[arg]
      ),
    });
  }
  setSortDirection() {
    this.setState(
      {
        sortDirection: this.state.sortDirection === "asc" ? "desc" : "asc",
      },
      this.setState({
        data: this.state.data.sort((a, b) =>
          this.state.sortDirection === "desc"
            ? a[this.state.value] - b[this.state.value]
            : b[this.state.value] - a[this.state.value]
        ),
      })
    );
  }

  render() {
    return (
      <React.Fragment>
        <div className="flex-row">
          <FaMapMarked className="detail-header-icon"></FaMapMarked>
          <div className="detail-header-description">Live</div>
          <div className="detail-header-time">
            <FaCircle style={{ width: "7px" }} />
            <Clock
              format={"HH:mm:ss"}
              ticking={true}
              timezone={"Europe/Berlin"}
              style={{ paddingLeft: "5px" }}
            />
          </div>
        </div>
        <div className="flex-row">
          <Filtersort
            data={this.props.data}
            handleChange={this.handleChange}
            column={this.state.value}
            sortDirection={this.state.sortDirection}
            setSortDirection={this.setSortDirection}
          />
        </div>

        <div className="">
          {this.state.data.map((datapoint) => (
            <Detailrow key={datapoint.data_id} datapoint={datapoint} />
          ))}
        </div>
      </React.Fragment>
    );
  }
}

export default Trucklist;
