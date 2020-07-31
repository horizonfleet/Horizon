import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Slider from "@material-ui/core/Slider";

const useStyles = makeStyles({
  root: {
    width: 300,
  },
});

export default function RangeSlider(props) {
  const classes = useStyles();
  const [value, setValue] = React.useState(props.delayFilter);

  const handleChange = (event, newValue) => {
    setValue(newValue);
    props.handleDelayChange(event, newValue);
  };

  const marks = [
    {
      value: 0,
      label: "0 min",
    },
    {
      value: 90,
      label: "90+ min",
    },
  ];
  return (
    <div className={classes.root}>
      <Typography id="range-slider" gutterBottom>
        Delay [Minutes]
      </Typography>
      <Slider
        value={value}
        onChange={handleChange}
        valueLabelDisplay="auto"
        aria-labelledby="discrete-slider-custom"
        min={0}
        max={90}
        step={5}
        marks={marks}
      />
    </div>
  );
}
